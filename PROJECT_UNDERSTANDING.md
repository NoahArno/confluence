# Confluence XML 迁移项目理解文档

## 项目概述

这是一个用于将 Atlassian Confluence 数据迁移到 XWiki 的工具集。项目通过解析 Confluence XML 导出包，将其转换为 XWiki 可以导入的格式。

**项目信息：**
- 组织：XWiki Contrib
- 当前版本：9.91.0
- 许可证：LGPL 2.1
- 最低 XWiki 版本：9.2
- 项目负责人：Thomas Mortagne
- 仓库：https://github.com/xwiki-contrib/confluence

---

## 核心架构

### 项目模块结构

项目采用 Maven 多模块架构，包含以下 8 个子模块：

```
confluence (父模块)
├── confluence-xml                          # 核心：XML 解析和过滤器
├── confluence-syntax-xhtml                 # XHTML 语法解析器
├── confluence-syntax-confluence            # Confluence Wiki 语法解析器
├── confluence-syntax-confluence10override  # Confluence 1.0 语法覆盖
├── confluence-resolvers                    # 引用解析器
├── confluence-resource-reference-type-parsers  # 资源引用类型解析器
├── confluence-url-mapping                  # URL 映射
└── confluence-url-mapping-scroll-viewport  # Scroll Viewport URL 映射
```

---

## 模块详解

### 1. confluence-xml（核心模块）

这是整个项目的核心模块，负责解析 Confluence XML 导出包并转换为 XWiki 格式。

**主要功能：**
- 解析 Confluence XML 导出包（ZIP 格式）
- 提供 Filter Stream 接口用于数据转换
- 处理页面、附件、用户、组、权限等实体
- 宏转换（Confluence 宏 → XWiki 宏）
- 引用转换（链接、图片、附件等）

**关键类：**

#### 输入处理层
- `ConfluenceXMLPackage` - 解析和准备 Confluence XML 包，处理 ZIP 文件解压、XML 解析、实体索引
- `ConfluenceInputFilterStream` - 主要的输入过滤流，协调整个导入过程
- `ConfluenceInputFilterStreamFactory` - 创建输入过滤流的工厂类
- `ConfluenceInputProperties` - 配置导入参数（源文件、目标空间、权限等）

#### 转换层
- `ConfluenceConverter` - 核心转换器，处理引用转换（页面、附件、用户等）
- `ConfluenceConverterListener` - 监听器，处理渲染事件并转换为 XWiki 格式
- `MacroConverter` - 宏转换接口，定义 Confluence 宏到 XWiki 宏的转换规则

#### 宏转换器（约 30+ 个）
位于 `internal.macros` 包，每个转换器处理特定的 Confluence 宏：
- `CodeMacroConverter` - 代码块宏
- `TocMacroConverter` - 目录宏
- `ChildrenMacroConverter` - 子页面列表宏
- `IncludeMacroConverter` - 包含页面宏
- `JiraMacroConverter` - JIRA 集成宏
- `MarkdownMacroConverter` - Markdown 宏
- 等等...

---

### 2. confluence-syntax-xhtml

解析 Confluence 的 XHTML 格式内容（Confluence 存储格式）。

**主要功能：**
- 解析 Confluence Storage Format（基于 XHTML）
- 支持 ADF (Atlassian Document Format) 解析
- 处理 Confluence 特有的 HTML 标签和属性
- 转换为 XWiki 渲染模型

**关键组件：**
- `ConfluenceXHTMLParser` - XHTML 解析器
- `ConfluenceXHTMLInputFilterStream` - XHTML 输入流
- 大量 TagHandler（50+ 个）处理各种 HTML 标签：
  - `MacroTagHandler` - 宏标签
  - `LinkTagHandler` - 链接标签
  - `ImageTagHandler` - 图片标签
  - `TableTagHandler` - 表格标签
  - `CodeTagHandler` - 代码标签
  - 等等...

---

### 3. confluence-syntax-confluence

解析 Confluence Wiki 标记语法（旧版本 Confluence 使用）。

**主要功能：**
- 解析 Confluence Wiki Markup 语法
- 基于 WikiModel 库实现
- 支持 Confluence 特有的 Wiki 语法元素

**关键类：**
- `ConfluenceParser` - Confluence Wiki 语法解析器
- `ConfluenceWikiParser` - WikiModel 集成
- `ConfluenceWikiReferenceParser` - Wiki 引用解析

---

### 4. confluence-resolvers

提供各种解析器，用于解析 Confluence 实体引用。

**主要解析器：**
- `ConfluencePageIdResolver` - 通过 ID 解析页面
- `ConfluencePageTitleResolver` - 通过标题解析页面
- `ConfluenceSpaceKeyResolver` - 解析空间键
- `ConfluenceSpaceResolver` - 解析空间引用
- `ConfluenceScrollTranslationResolver` - Scroll 翻译解析
- `ConfluenceResourceReferenceResolver` - 资源引用解析

---

## 工作流程

### 整体迁移流程

```
1. 准备阶段
   ↓
   用户从 Confluence 导出 XML 包（ZIP 文件）
   ↓
2. 解析阶段
   ↓
   ConfluenceXMLPackage 解压并解析 XML
   - 读取 entities.xml（主索引文件）
   - 解析空间、页面、用户、组等实体
   - 建立实体关系映射
   ↓
3. 转换阶段
   ↓
   ConfluenceInputFilterStream 处理每个实体
   - 空间 → XWiki Space
   - 页面 → XWiki Document
   - 附件 → XWiki Attachment
   - 用户/组 → XWiki User/Group
   ↓
4. 内容转换
   ↓
   根据内容格式选择解析器：
   - XHTML → ConfluenceXHTMLParser
   - Wiki Markup → ConfluenceParser
   ↓
   宏转换（MacroConverter）
   引用转换（ConfluenceConverter）
   ↓
5. 输出阶段
   ↓
   通过 Filter Stream API 输出到 XWiki
```

---

## 核心概念

### 1. Filter Stream API

项目基于 XWiki 的 Filter Stream API 构建，这是一个事件驱动的数据转换框架：

- **InputFilterStream** - 读取源数据并生成事件
- **OutputFilterStream** - 接收事件并写入目标系统
- **Filter** - 定义事件接口（WikiDocumentFilter, UserFilter 等）

### 2. 实体映射

Confluence 实体到 XWiki 实体的映射：

| Confluence | XWiki |
|------------|-------|
| Space | Space |
| Page | Document (WebHome) |
| Blog Post | Document (带 Blog 类) |
| Attachment | Attachment |
| User | User (XWiki.UserName) |
| Group | Group |
| Comment | Comment Object |
| Label | Tag |

### 3. 宏转换策略

宏转换采用策略模式，每个 MacroConverter 实现负责一个或多个 Confluence 宏：

- **直接映射** - Confluence 宏直接对应 XWiki 宏（如 code → code）
- **参数转换** - 宏名相同但参数需要转换
- **复杂转换** - 需要解析内容或生成新结构
- **降级处理** - 无对应宏时使用 HTML 或警告信息

---

## 关键技术点

### 1. XML 解析

使用 StAX (Streaming API for XML) 进行流式解析：
- 内存效率高，适合大文件
- XMLStreamReader 逐个读取 XML 元素
- 支持增量处理

### 2. 内容格式支持

**Confluence Storage Format (XHTML):**
- Confluence 5.0+ 的默认存储格式
- 基于 XHTML，带 Confluence 特有命名空间
- 使用 WikiModel 解析 HTML 并转换为事件

**Confluence Wiki Markup:**
- 旧版 Confluence 使用的纯文本标记
- 类似 MediaWiki 语法
- 通过 WikiModel 库解析

**ADF (Atlassian Document Format):**
- Confluence Cloud 的新格式
- JSON 结构
- 项目支持基本的 ADF 解析

### 3. 引用解析

处理各种引用类型：
- **页面引用** - `[Page Title]`, `[Space:Page]`, `[^attachment.pdf]`
- **附件引用** - 图片、文件链接
- **用户引用** - `@username`, `~username`
- **外部链接** - URL 转换和映射

### 4. 权限处理

支持 Confluence 权限迁移：
- Space 级别权限
- Page 级别权限
- 转换为 XWiki 权限模型

---

## 配置选项

### ConfluenceInputProperties 主要参数

- `source` - Confluence XML 包路径
- `spacePageName` - 目标空间的主页名称
- `terminal` - 是否为终端空间
- `convertToXWiki` - 是否转换为 XWiki 语法
- `macroContentSyntax` - 宏内容语法
- `linkMapping` - 链接映射配置
- `userIdMapping` - 用户 ID 映射
- `groupMapping` - 组映射
- `storeConfluenceDetailsInClass` - 是否存储 Confluence 元数据

---

## 扩展点

### 如何添加自定义宏转换器

1. 创建类实现 `MacroConverter` 接口
2. 使用 `@Component` 和 `@Named` 注解
3. 实现 `toXWiki()` 方法
4. 在 `pom.xml` 中声明组件

示例结构：
```java
@Component
@Named("mymacro")
@Singleton
public class MyMacroConverter extends AbstractMacroConverter {
    @Override
    public void toXWiki(String id, Map<String, String> parameters,
                        String content, boolean inline, Listener listener) {
        // 转换逻辑
    }
}
```

### 如何自定义引用转换

扩展 `ConfluenceFilterReferenceConverter` 或 `ConfluenceURLConverter`：
- 处理特殊的链接格式
- 自定义页面/空间映射规则
- 处理外部系统集成

---

## 常见问题和解决方案

### 1. 不兼容的宏

**问题：** Confluence 宏在 XWiki 中没有对应实现

**解决方案：**
- 创建自定义 MacroConverter
- 使用 HTML 宏包装原始内容
- 使用 info/warning 宏显示迁移提示

### 2. 页面层级结构

**问题：** Confluence 扁平结构 vs XWiki 层级结构

**解决方案：**
- 使用页面父子关系重建层级
- 配置 `terminal` 参数控制空间类型
- 使用 `spacePageName` 自定义主页名称

### 3. 附件引用

**问题：** 附件路径和引用方式不同

**解决方案：**
- LinkMapper 处理附件路径映射
- 自动转换附件引用格式
- 保留原始附件文件名

### 4. 用户和权限

**问题：** 用户名格式和权限模型差异

**解决方案：**
- UsernameCleaner 清理用户名
- 用户/组映射配置
- 权限转换规则

---

## 二次开发建议

### 针对你的迁移需求

基于你提到的"不兼容问题"，以下是二次开发的切入点：

#### 1. 识别不兼容内容
- 在日志中查找 "Unknown macro" 或 "Unsupported" 警告
- 检查迁移后的页面，找出格式错误的内容
- 使用 ConfluenceFilteringEvent 监听迁移过程

#### 2. 扩展宏转换
- 在 `confluence-xml/src/main/java/.../internal/macros/` 添加新的转换器
- 参考现有转换器（如 `CodeMacroConverter`）的实现模式
- 注册到 Spring 组件系统

#### 3. 自定义内容处理
- 扩展 `ConfluenceXHTMLParser` 处理特殊 HTML 结构
- 添加新的 TagHandler 处理自定义标签
- 修改 `ConfluenceConverter` 调整引用转换逻辑

#### 4. 调试和测试
- 使用单元测试验证转换逻辑
- 参考 `src/test/resources` 中的测试用例
- 启用详细日志：`org.xwiki.contrib.confluence` 设为 DEBUG

---

## 项目文件统计

- confluence-xml 模块：86 个 Java 文件
- confluence-syntax-xhtml 模块：约 80+ 个 Java 文件
- 总计：约 200+ 个 Java 类
- 主要包结构：
  - `org.xwiki.contrib.confluence.filter` - 核心过滤器
  - `org.xwiki.contrib.confluence.filter.input` - 输入处理
  - `org.xwiki.contrib.confluence.filter.internal.macros` - 宏转换
  - `org.xwiki.contrib.confluence.parser.xhtml` - XHTML 解析
  - `org.xwiki.contrib.confluence.resolvers` - 引用解析

---

## 下一步行动

1. **定位问题** - 确定具体的不兼容内容类型（宏、标签、格式等）
2. **分析现有代码** - 查看相关的转换器实现
3. **设计解决方案** - 决定是修改现有转换器还是添加新的
4. **实现和测试** - 编写代码并用实际数据测试
5. **集成部署** - 打包并部署到 XWiki 环境

---

*文档生成时间：2026-03-12*
*项目版本：9.91.0*

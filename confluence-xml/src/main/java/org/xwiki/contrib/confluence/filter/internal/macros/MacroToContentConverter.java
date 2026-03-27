/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.contrib.confluence.filter.internal.macros;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.confluence.filter.ConversionException;
import org.xwiki.contrib.confluence.filter.input.ConfluenceInputContext;
import org.xwiki.rendering.listener.ListType;
import org.xwiki.rendering.listener.Listener;
import org.xwiki.rendering.listener.QueueListener;
import org.xwiki.rendering.listener.chaining.EventType;

/**
 * Converts a macro call to a group, retaining the id, class and content.
 *
 * @version $Id$
 * @since 9.51.1
 */
@Component(hints = { "ul", "legend", "auihorizontalnav", "auibuttongroup", "auihorizontalnavpage", "tableenhancer",
    "footnote", "task-list" })
@Singleton
public class MacroToContentConverter extends AbstractParseContentMacroConverter
{
    private static final String TASK_LIST_MACRO = "task-list";

    private static final String TABLE_ENHANCER_MACRO = "tableenhancer";

    private static final String TASK_MACRO = "task";

    private static final String STATUS_PARAMETER = "status";

    private static final String COMPLETE_STATUS = "complete";

    private static final String CHECKED_MARKER = "[x]";

    private static final String UNCHECKED_MARKER = "[ ]";

    private static final Pattern PLACEHOLDER_INLINE_TASKS_PATTERN =
        Pattern.compile("(?m)^\\s*\\(%\\s*class=\"placeholder-inline-tasks\"\\s*%\\)\\s*");

    private static final String DIV_CLASS_FORMAT = "confluence_%s_content";

    private static final String HTML_ATTRIBUTE_ID = "id";

    private static final String HTML_ATTRIBUTE_CLASS = "class";

    @Inject
    private ConfluenceInputContext inputContext;

    private static class TaskItem
    {
        private final String status;

        private final String content;

        TaskItem(String status, String content)
        {
            this.status = status;
            this.content = content;
        }
    }

    @Override
    public String toXWikiId(String confluenceId, Map<String, String> confluenceParameters, String confluenceContent,
        boolean inline)
    {
        return confluenceId;
    }

    @Override
    public void toXWiki(String id, Map<String, String> parameters, boolean inline, String content, Listener listener)
        throws ConversionException
    {
        if (TASK_LIST_MACRO.equals(id) && isTaskListConversionEnabled()) {
            if (convertTaskListToCheckboxList(id, content, listener)) {
                return;
            }
        }

        if (TABLE_ENHANCER_MACRO.equals(id)) {
            // XWiki does not provide this macro out of the box: keep only the body content (table, etc.).
            parseContent(id, listener, content);
            return;
        }

        Map<String, String> divWrapperParams = toXWikiParameters(id, parameters, content);
        String newContent = toXWikiContent(id, parameters, content);
        beginEvent(id, divWrapperParams, newContent, inline, listener);
        parseContent(id, listener, newContent);
        endEvent(id, divWrapperParams, newContent, inline, listener);
    }

    /**
     * The content is about to be processed (parseContent is about to be called).
     * @param id the Confluence macro name
     * @param parameters the Confluence parameters
     * @param content the content of the macro
     * @param inline whether the macro is parsed inline
     * @param listener the listener
     * @throws ConversionException if the conversion cannot continue
     */
    protected void beginEvent(String id, Map<String, String> parameters, String content, boolean inline,
        Listener listener) throws ConversionException
    {
        listener.beginGroup(parameters);
    }

    /**
     * The content was just processed (parseContent just returned).
     * @param id the Confluence macro name
     * @param parameters the Confluence parameters
     * @param content the content of the macro
     * @param inline whether the macro is parsed inline
     * @param listener the listener
     * @throws ConversionException if the conversion cannot continue
     */
    protected void endEvent(String id, Map<String, String> parameters, String content, boolean inline,
        Listener listener) throws ConversionException
    {
        listener.endGroup(parameters);
    }

    @Override
    protected Map<String, String> toXWikiParameters(String confluenceId, Map<String, String> confluenceParameters,
        String content) throws ConversionException
    {
        Map<String, String> divWrapperParams = new HashMap<>();

        List<String> classes = new ArrayList<>();
        classes.add(String.format(DIV_CLASS_FORMAT, confluenceId));
        String className = confluenceParameters.get(HTML_ATTRIBUTE_CLASS);
        if (StringUtils.isNotEmpty(className)) {
            classes.add(className);
        }
        divWrapperParams.put(HTML_ATTRIBUTE_CLASS, String.join(" ", classes));

        String id = confluenceParameters.get(HTML_ATTRIBUTE_ID);
        if (StringUtils.isNotEmpty(id)) {
            divWrapperParams.put(HTML_ATTRIBUTE_ID, id);
        }
        return divWrapperParams;
    }

    @Override
    public InlineSupport supportsInlineMode(String id, Map<String, String> parameters, String content)
    {
        return InlineSupport.NO;
    }

    private boolean convertTaskListToCheckboxList(String id, String content, Listener listener)
    {
        List<TaskItem> tasks = new ArrayList<>();

        QueueListener taskListQueue = new QueueListener();
        parseContent(id, taskListQueue, content);

        for (QueueListener.Event event : taskListQueue) {
            if (event.eventType != EventType.ON_MACRO) {
                continue;
            }

            String macroId = (String) event.eventParameters[0];
            @SuppressWarnings("unchecked")
            Map<String, String> macroParameters = (Map<String, String>) event.eventParameters[1];
            String macroContent = (String) event.eventParameters[2];
            boolean macroInline = (boolean) event.eventParameters[3];

            if (!macroInline && TASK_MACRO.equals(macroId)) {
                tasks.add(new TaskItem(macroParameters.get(STATUS_PARAMETER), macroContent));
            } else {
                return false;
            }
        }

        if (tasks.isEmpty()) {
            return false;
        }

        listener.beginList(ListType.BULLETED, Listener.EMPTY_PARAMETERS);
        for (TaskItem task : tasks) {
            listener.beginListItem();
            listener.onWord(COMPLETE_STATUS.equalsIgnoreCase(task.status) ? CHECKED_MARKER : UNCHECKED_MARKER);

            String taskContent = cleanTaskContent(task.content);
            if (StringUtils.isNotBlank(taskContent)) {
                listener.onSpace();
                parseContent(TASK_MACRO, listener, taskContent);
            }
            listener.endListItem();
        }
        listener.endList(ListType.BULLETED, Listener.EMPTY_PARAMETERS);

        return true;
    }

    private String cleanTaskContent(String taskContent)
    {
        String cleanedTaskContent = StringUtils.defaultString(taskContent);
        cleanedTaskContent = PLACEHOLDER_INLINE_TASKS_PATTERN.matcher(cleanedTaskContent).replaceAll("");
        return StringUtils.strip(cleanedTaskContent, "\r\n");
    }

    private boolean isTaskListConversionEnabled()
    {
        return this.inputContext != null
            && this.inputContext.getProperties() != null
            && this.inputContext.getProperties().isTaskListAsCheckboxListEnabled();
    }
}

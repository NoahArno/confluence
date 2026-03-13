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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.confluence.filter.ConversionException;
import org.xwiki.rendering.listener.Listener;

/**
 * Converts Confluence layout sections to XWiki groups using Bootstrap classes.
 *
 * @version $Id$
 */
@Component
@Singleton
@Named("layout-section")
public class LayoutSectionMacroConverter extends AbstractParseContentMacroConverter
{
    private static final Pattern LAYOUT_CELL_PATTERN =
        Pattern.compile("\\{\\{layout-cell(?:\\s+[^}]*)?\\}\\}(.*?)\\{\\{/layout-cell\\}\\}", Pattern.DOTALL);

    private static final String TYPE_PARAMETER = "ac:type";

    private static final String TYPE_PARAMETER_FALLBACK = "type";

    private static final String CLASS_PARAMETER = "class";

    private static final String ROW_CLASS = "row";

    private static final Map<String, int[]> TYPE_TO_WIDTHS = createTypeToWidths();

    @Override
    public String toXWikiId(String confluenceId, Map<String, String> confluenceParameters, String confluenceContent,
        boolean inline)
    {
        return null;
    }

    @Override
    protected void toXWiki(String confluenceId, Map<String, String> confluenceParameters, boolean inline,
        String confluenceContent, Listener listener) throws ConversionException
    {
        Map<String, String> rowParameters = Collections.singletonMap(CLASS_PARAMETER, ROW_CLASS);
        listener.beginGroup(rowParameters);
        try {
            List<String> cellContents = extractCellContents(confluenceContent);
            if (cellContents.isEmpty()) {
                parseContent(confluenceId, listener, confluenceContent);
                return;
            }

            int[] widths = computeWidths(confluenceParameters, cellContents.size());
            for (int i = 0; i < cellContents.size(); i++) {
                String cellClass = "col-xs-12 col-sm-" + widths[i];
                Map<String, String> cellParameters = Collections.singletonMap(CLASS_PARAMETER, cellClass);
                listener.beginGroup(cellParameters);
                try {
                    parseContent(confluenceId, listener, cellContents.get(i));
                } finally {
                    listener.endGroup(cellParameters);
                }
            }
        } finally {
            listener.endGroup(rowParameters);
        }
    }

    @Override
    protected Map<String, String> toXWikiParameters(String confluenceId, Map<String, String> confluenceParameters,
        String content)
    {
        return Collections.emptyMap();
    }

    @Override
    public InlineSupport supportsInlineMode(String id, Map<String, String> parameters, String content)
    {
        return InlineSupport.NO;
    }

    private static List<String> extractCellContents(String confluenceContent)
    {
        List<String> result = new ArrayList<>();
        Matcher matcher = LAYOUT_CELL_PATTERN.matcher(StringUtils.defaultString(confluenceContent));
        while (matcher.find()) {
            result.add(matcher.group(1));
        }
        return result;
    }

    private int[] computeWidths(Map<String, String> confluenceParameters, int cellCount)
    {
        if (cellCount <= 0) {
            return new int[0];
        }

        String type = confluenceParameters.get(TYPE_PARAMETER);
        if (StringUtils.isEmpty(type)) {
            type = confluenceParameters.get(TYPE_PARAMETER_FALLBACK);
        }

        int[] mappedWidths = TYPE_TO_WIDTHS.get(type);
        if (mappedWidths == null || mappedWidths.length != cellCount) {
            if (StringUtils.isNotEmpty(type)) {
                markUnhandledParameterValue(confluenceParameters, TYPE_PARAMETER);
            }
            return generateEvenWidths(cellCount);
        }

        return mappedWidths;
    }

    private static int[] generateEvenWidths(int cellCount)
    {
        int[] widths = new int[cellCount];
        int base = 12 / cellCount;
        int remainder = 12 % cellCount;
        for (int i = 0; i < cellCount; i++) {
            widths[i] = base + (i < remainder ? 1 : 0);
        }
        return widths;
    }

    private static Map<String, int[]> createTypeToWidths()
    {
        Map<String, int[]> map = new HashMap<>();
        map.put("single", new int[] { 12 });
        map.put("two_equal", new int[] { 6, 6 });
        map.put("two_left_sidebar", new int[] { 4, 8 });
        map.put("two_right_sidebar", new int[] { 8, 4 });
        map.put("three_equal", new int[] { 4, 4, 4 });
        map.put("three_with_sidebars", new int[] { 2, 8, 2 });
        map.put("three_with_left_sidebar", new int[] { 2, 2, 8 });
        map.put("three_with_right_sidebar", new int[] { 8, 2, 2 });
        return map;
    }
}

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

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.confluence.filter.AbstractMacroConverter;
import org.xwiki.contrib.confluence.filter.ConversionException;
import org.xwiki.rendering.listener.Listener;

/**
 * Convert Confluence ui-button macro to XWiki button macro.
 *
 * @version $Id$
 * @since 9.96.0
 */
@Component
@Singleton
@Named("ui-button")
public class UIButtonMacroConverter extends AbstractMacroConverter
{
    private static final String OUTPUT_TYPE_PARAMETER = "atlassian-macro-output-type";

    private static final String INLINE_OUTPUT_TYPE = "INLINE";

    @Override
    public String toXWikiId(String confluenceId, Map<String, String> confluenceParameters, String confluenceContent,
        boolean inline)
    {
        return "button";
    }

    @Override
    protected Map<String, String> toXWikiParameters(String confluenceId, Map<String, String> confluenceParameters,
        String content)
    {
        Map<String, String> parameters = new LinkedHashMap<>(8);
        saveParameter(confluenceParameters, parameters, "title", true);
        saveParameter(confluenceParameters, parameters, "tooltip", true);
        String link = saveParameter(confluenceParameters, parameters, "url", "link", true);
        if (link == null) {
            parameters.put("link", " ");
        }
        saveUppercaseParameter(confluenceParameters, parameters, "icon");
        saveUppercaseParameter(confluenceParameters, parameters, "color");
        saveUppercaseParameterWithDefault(confluenceParameters, parameters, "size", "MEDIUM");
        saveUppercaseParameterWithDefault(confluenceParameters, parameters, "display", "INLINE");
        return parameters;
    }

    @Override
    public InlineSupport supportsInlineMode(String id, Map<String, String> parameters, String content)
    {
        String outputType = parameters.get(OUTPUT_TYPE_PARAMETER);
        return INLINE_OUTPUT_TYPE.equalsIgnoreCase(outputType) ? InlineSupport.YES : InlineSupport.NO;
    }

    @Override
    protected void toXWiki(String confluenceId, Map<String, String> confluenceParameters, boolean inline,
        String confluenceContent, Listener listener) throws ConversionException
    {
        boolean renderInline = INLINE_OUTPUT_TYPE.equalsIgnoreCase(confluenceParameters.get(OUTPUT_TYPE_PARAMETER));

        if (!renderInline) {
            listener.onNewLine();
        }

        super.toXWiki(confluenceId, confluenceParameters, renderInline, confluenceContent, listener);

        if (!renderInline) {
            listener.onNewLine();
        }
    }

    private void saveUppercaseParameter(Map<String, String> confluenceParameters, Map<String, String> xwikiParameters,
        String parameterName)
    {
        String value = saveParameter(confluenceParameters, xwikiParameters, parameterName, true);
        if (value != null) {
            xwikiParameters.put(parameterName, value.toUpperCase(Locale.ROOT));
        }
    }

    private void saveUppercaseParameterWithDefault(Map<String, String> confluenceParameters,
        Map<String, String> xwikiParameters, String parameterName, String defaultValue)
    {
        String value = saveParameter(confluenceParameters, xwikiParameters, parameterName, true);
        if (value == null) {
            xwikiParameters.put(parameterName, defaultValue);
        } else {
            xwikiParameters.put(parameterName, value.toUpperCase(Locale.ROOT));
        }
    }
}

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

import java.util.Collections;
import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.confluence.filter.AbstractMacroConverter;
import org.xwiki.contrib.confluence.filter.ConversionException;
import org.xwiki.rendering.listener.Listener;

/**
 * Converts Confluence {@code <time datetime="..."/>} to plain text.
 * This avoids generating an unknown {@code [time]} macro in XWiki.
 *
 * @version $Id$
 */
@Component
@Singleton
@Named("time")
public class TimeMacroConverter extends AbstractMacroConverter
{
    private static final String DATETIME = "datetime";

    @Override
    public String toXWikiId(String confluenceId, Map<String, String> confluenceParameters, String confluenceContent,
        boolean inline)
    {
        return confluenceId;
    }

    @Override
    protected void toXWiki(String confluenceId, Map<String, String> confluenceParameters, boolean inline,
        String confluenceContent, Listener listener) throws ConversionException
    {
        String datetime = confluenceParameters.get(DATETIME);
        if (StringUtils.isNotBlank(datetime)) {
            listener.onWord(datetime);
        } else if (StringUtils.isNotBlank(confluenceContent)) {
            listener.onWord(confluenceContent);
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
        return InlineSupport.YES;
    }
}

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

import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.confluence.filter.AbstractMacroConverter;
import org.xwiki.contrib.confluence.filter.ConversionException;

/**
 * Convert Confluence excerpt-include macro parameter name to the XWiki expected one.
 *
 * @version $Id$
 * @since 9.96.0
 */
@Component
@Singleton
@Named("excerpt-include")
public class ExcerptIncludeMacroConverter extends AbstractMacroConverter
{
    @Override
    public String toXWikiId(String confluenceId, Map<String, String> confluenceParameters, String confluenceContent,
        boolean inline)
    {
        return "excerpt-include";
    }

    @Override
    protected Map<String, String> toXWikiParameters(String confluenceId, Map<String, String> confluenceParameters,
        String content) throws ConversionException
    {
        String reference = confluenceParameters.get("");
        if (StringUtils.isEmpty(reference)) {
            reference = confluenceParameters.get("0");
        }

        if (StringUtils.isEmpty(reference)) {
            throw new ConversionException(
                "Missing parameter for the excerpt-include macro, killing the macro conversion");
        }

        return Map.of("reference", reference);
    }

    @Override
    public InlineSupport supportsInlineMode(String id, Map<String, String> parameters, String content)
    {
        return InlineSupport.YES;
    }
}

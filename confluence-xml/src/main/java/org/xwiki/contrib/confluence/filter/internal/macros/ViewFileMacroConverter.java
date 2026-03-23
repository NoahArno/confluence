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
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.reference.ResourceType;

/**
 * Convert Confluence view-file macro to an attachment link.
 *
 * @version $Id$
 * @since 9.96.0
 */
@Component
@Singleton
@Named("view-file")
public class ViewFileMacroConverter extends AbstractMacroConverter
{
    private static final String ATTACHMENT_FILENAME_PARAMETER = "att--filename";

    private static final String LINK_LABEL_PREFIX = "view-file：";

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
        String attachmentReference = confluenceParameters.get(ATTACHMENT_FILENAME_PARAMETER);
        if (StringUtils.isBlank(attachmentReference)) {
            super.toXWiki(confluenceId, confluenceParameters, inline, confluenceContent, listener);
            return;
        }

        ResourceReference attachmentResourceReference =
            new ResourceReference(attachmentReference, ResourceType.ATTACHMENT);
        listener.onNewLine();
        listener.beginLink(attachmentResourceReference, false, Listener.EMPTY_PARAMETERS);
        listener.onWord(LINK_LABEL_PREFIX + extractAttachmentFilename(attachmentReference));
        listener.endLink(attachmentResourceReference, false, Listener.EMPTY_PARAMETERS);
        listener.onNewLine();
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

    private String extractAttachmentFilename(String attachmentReference)
    {
        int lastSeparator = getLastUnescapedAt(attachmentReference);
        String filename = lastSeparator >= 0 ? attachmentReference.substring(lastSeparator + 1) : attachmentReference;
        return filename.replace("\\@", "@").replace("\\\\", "\\");
    }

    private int getLastUnescapedAt(String value)
    {
        int foundIndex = -1;
        boolean escaped = false;
        for (int i = 0; i < value.length(); ++i) {
            char currentChar = value.charAt(i);
            if (escaped) {
                escaped = false;
                continue;
            }

            if (currentChar == '\\') {
                escaped = true;
            } else if (currentChar == '@') {
                foundIndex = i;
            }
        }
        return foundIndex;
    }
}

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
import java.util.Locale;
import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.confluence.filter.AbstractMacroConverter;

/**
 * Convert the confluence ui-text-box macro to a box macro.
 *
 * @version $Id$
 * @since 1.90.0
 */
@Component
@Singleton
@Named("ui-text-box")
public class UITextBoxMacroConverter extends AbstractMacroConverter
{
    private static final String ICON_PARAMETER = "icon";

    private static final String ICON_NOTE = "note";

    private static final String ICON_DEFAULT = "default";

    private static final String ICON_INFO = "info";

    private static final String ICON_TIP = "tip";

    private static final String ICON_WARNING = "warning";

    @Override
    public String toXWikiId(String confluenceId, Map<String, String> confluenceParameters, String confluenceContent,
        boolean inline)
    {
        String icon = confluenceParameters.get(ICON_PARAMETER);
        if (icon == null) {
            return "box";
        }

        switch (icon.toLowerCase(Locale.ROOT)) {
            case ICON_NOTE:
                return "warning";
            case ICON_DEFAULT:
                return "box";
            case ICON_INFO:
                return "info";
            case ICON_TIP:
                return "success";
            case ICON_WARNING:
                return "error";
            default:
                return "box";
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
}

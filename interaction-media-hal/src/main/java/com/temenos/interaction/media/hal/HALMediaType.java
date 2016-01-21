package com.temenos.interaction.media.hal;

/*
 * #%L
 * interaction-media-hal
 * %%
 * Copyright (C) 2012 - 2013 Temenos Holdings N.V.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */

import javax.ws.rs.core.MediaType;

public class HALMediaType {

    /** "application/hal+xml" */
    public final static String APPLICATION_HAL_XML = "application/hal+xml";
    /** "application/hal+xml" */
    public final static MediaType APPLICATION_HAL_XML_TYPE = new MediaType("application","hal+xml");

    /** "application/hal+json" */
    public final static String APPLICATION_HAL_JSON = "application/hal+json";
    /** "application/hal+json" */
    public final static MediaType APPLICATION_HAL_JSON_TYPE = new MediaType("application","hal+json");

	public static String baseMediaType( MediaType parameterisedMediaType ) {
		return String.format("%s/%s", parameterisedMediaType.getType(), parameterisedMediaType.getSubtype());
	}
	public static String charset( MediaType parameterisedMediaType, String defaultMediaType ) {
		String specified = parameterisedMediaType.getParameters().get("charset");
		if ( specified == null )
			return defaultMediaType;
		else
			return specified;
	}
}

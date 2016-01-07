package com.temenos.interaction.core.hypermedia;

/*
 * #%L
 * interaction-core
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


import java.util.List;

public class CollectionResourceState extends ResourceState {
    private static final String[] RELATIONS = new String[]{ "collection" };

	public CollectionResourceState(String entityName, String name, List<Action> actions, String path) {
		super(entityName, name, actions, path, RELATIONS);
	}
	public CollectionResourceState(String entityName, String name, List<Action> actions, String path, UriSpecification uriSpec) {
		super(entityName, name, actions, path, RELATIONS, uriSpec);
	}
	public CollectionResourceState(String entityName, String name, List<Action> actions, String path, String[] rels, UriSpecification uriSpec) {
		super(entityName, name, actions, path, rels != null ? rels : RELATIONS, uriSpec);
	}
	public CollectionResourceState(String entityName, String name, List<Action> actions, String path, String[] rels, UriSpecification uriSpec, ResourceState errorState) {
		super(entityName, name, actions, path, rels != null ? rels : RELATIONS, uriSpec, errorState);
	}
	
}

package com.temenos.interaction.media.hal;

/*
 * #%L
 * interaction-media-hal
 * %%
 * Copyright (C) 2012 - 2016 Temenos Holdings N.V.
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

import java.util.ArrayList;
import java.util.List;

import com.temenos.interaction.core.entity.Entity;
import com.temenos.interaction.core.entity.EntityMetadata;
import com.temenos.interaction.core.entity.EntityProperty;
import com.temenos.interaction.core.entity.EntityProperties;
import com.temenos.interaction.core.entity.Metadata;
import com.temenos.interaction.core.entity.MetadataParser;
import com.temenos.interaction.core.entity.vocabulary.TermFactory;
import com.temenos.interaction.core.entity.vocabulary.Vocabulary;
import com.temenos.interaction.core.entity.vocabulary.terms.TermComplexGroup;
import com.temenos.interaction.core.entity.vocabulary.terms.TermComplexType;
import com.temenos.interaction.core.entity.vocabulary.terms.TermValueType;

public class EmptyEntity {
	/**
	 * Create an empty entity with all available properties
	 * 
	 * @param entityMetadata
	 * @return
	 */
	public EntityProperties getProperties(EntityMetadata entityMetadata) {
		EntityProperties entityProperties = new EntityProperties();
		for (String propertyName : entityMetadata.getTopLevelProperties()) {
			Vocabulary vocab = entityMetadata.getPropertyVocabulary(propertyName);
			if (entityMetadata.isPropertyComplex(propertyName)) {
				entityProperties.setProperty(new EntityProperty(propertyName, getComplexProperty(propertyName,
						entityMetadata)));
			} else if (vocab.getTerm(TermComplexGroup.TERM_NAME) == null) {
				entityProperties.setProperty(entityMetadata.createEmptyEntityProperty(propertyName));
			}
		}
		return entityProperties;
	}
	/**
	 * Return empty complex type entity
	 * 
	 * @param propertyName
	 * @param entityMetadata
	 * @return
	 */
	protected List<EntityProperties> getComplexProperty(String propertyName, EntityMetadata entityMetadata) {
		List<EntityProperties> complexProperties = new ArrayList<EntityProperties>();
		EntityProperties entityProperties = new EntityProperties();
		for (String propName : getComplexElements(propertyName, entityMetadata)) {
			String fullyQualifiedPropertyName = propertyName + "." + propName;
			if (entityMetadata.isPropertyComplex(fullyQualifiedPropertyName)) {
				entityProperties.setProperty(new EntityProperty(propName, getComplexProperty(
						fullyQualifiedPropertyName, entityMetadata)));
			} else {
				entityProperties.setProperty(entityMetadata.createEmptyEntityProperty(propName));
			}
		}
		complexProperties.add(entityProperties);
		return complexProperties;
	}

	/**
	 * Return empty complex type elements
	 * 
	 * @param complexPropertyName
	 * @param entityMetadata
	 * @return
	 */
	protected List<String> getComplexElements(String complexPropertyName, EntityMetadata entityMetadata) {
		if (!entityMetadata.isPropertyComplex(complexPropertyName)) {
			throw new RuntimeException("[" + complexPropertyName + "] is not a complex property.");
		}
		List<String> properties = new ArrayList<String>();
		for (String propName : entityMetadata.getPropertyVocabularyKeySet()) {
			String complexGroup = entityMetadata.getPropertyComplexGroup(propName);
			if (complexGroup != null && complexGroup.equals(complexPropertyName)) {
				properties.add(entityMetadata.getSimplePropertyName(propName));
			}
		}
		return properties;
	}

}

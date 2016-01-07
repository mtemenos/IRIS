package com.temenos.interaction.core.command;

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


import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

import javax.ws.rs.core.GenericEntity;

import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl;

import com.temenos.interaction.core.entity.Entity;
import com.temenos.interaction.core.resource.EntityResource;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;


public class CommandHelper {
	
	/**
	 * Create an Entity entity resource (entry)
	 * @param e Entity
	 * @return entity resource
	 */
	public static EntityResource<Entity> createEntityResource(Entity e) {
		String entityName = e != null ? e.getName() : null;
		return createEntityResource(entityName, e, Entity.class);
	}
	
	/**
	 * Create a new entity resource.
	 * @param entity entity
	 * @param entityType
	 * @return entity resource
	 */
	public static<E> EntityResource<E> createEntityResource(E entity, Class<?> entityType) {
		return createEntityResource(null, entity, entityType);
	}

	/**
	 * Create a new entity resource.
	 * @param entityName entity name
	 * @param entity entity
	 * @param entityType entity type - this should match the type of the template parameter 'E'
	 * @return entity resource
	 */
	public static<E> EntityResource<E> createEntityResource(String entityName, final E entity, final Class<?> entityType) {
		return new EntityResource<E>(entityName, entity) {
			@Override
			public GenericEntity<EntityResource<E>> getGenericEntity() {
				//Override the generic type to be the type of the entity rather than 'E'
				Type genericType = entityType != null ? getEffectiveGenericType(this.getClass().getGenericSuperclass(), entity, entityType) : getEffectiveGenericType(this.getClass().getGenericSuperclass(), entity);
				return new GenericEntity<EntityResource<E>>(this, genericType);
			}
		};
	}
	
	/*
	 * Returns the type of the specified entity.
	 * This method will try to evaluate entity type E. If entity type E implements exactly one interface
	 * it will use the interface type, otherwise it will use the class type of entity type E.
	 * @param genericType generic type
	 * @param entity entity
	 * @return type
	 */
	public static<E> Type getEffectiveGenericType(final Type superClassType, final E entity) {
		Class<?> entityType = entity.getClass();
		Class<?> entityInterfaces[] = entityType.getInterfaces();
		if(entityInterfaces != null && entityInterfaces.length == 1) {
			entityType = entityInterfaces[0];
		}
		return getEffectiveGenericType(superClassType, entity, entityType);
	}
	
	/*
	 * Returns the type of the specified entity.
	 * @param superClassType parent class of generic type
	 * @param entity entity
	 * @param entityType entity type - this should match the type of the template parameter 'E'
	 * @return type
	 */
	@SuppressWarnings("rawtypes")
	private static<E> Type getEffectiveGenericType(final Type superClassType, final E entity, final Class<?> entityType) {
		Type newGenericType;
		if(superClassType instanceof ParameterizedType) {
			ParameterizedType parametrizedType = (ParameterizedType) superClassType;
			Type[] types = parametrizedType.getActualTypeArguments();
			Type[] newActualTypeArguments = new Type[types.length];
			for(int i=0; i < types.length; i++) {
				Type type = types[i];
				if(type instanceof TypeVariable) {
					final TypeVariable<?> typeVar = (TypeVariable<?>) type;
					newActualTypeArguments[i] = (TypeVariable) Proxy.newProxyInstance(
                                                typeVar.getClass().getClassLoader(), 
                                                new Class[]{TypeVariable.class}, 
                                                new TypeVariableHandler(typeVar, entityType));
				}
			}
			newGenericType = ParameterizedTypeImpl.make((Class<?>) parametrizedType.getRawType(), newActualTypeArguments, parametrizedType.getOwnerType());
		} else if (superClassType instanceof TypeVariable) {
			final TypeVariable<?> typeVar = (TypeVariable<?>) superClassType;
			Type t = (TypeVariable) Proxy.newProxyInstance(
                                                typeVar.getClass().getClassLoader(), 
                                                new Class[]{TypeVariable.class}, 
                                                new TypeVariableHandler(typeVar, entityType));
			newGenericType = t; 
		}
		else {
			newGenericType = superClassType;
		}
		return newGenericType;
	}
        
        private static class TypeVariableHandler implements InvocationHandler {

            private final TypeVariable<?> typeVar;
            private final Class<?> entityType;

            public TypeVariableHandler(TypeVariable<?> typeVar, Class<?> entityType) {
                this.typeVar = typeVar;
                this.entityType = entityType;
            }

            @Override
            public Object invoke(Object o, Method method, Object[] os) throws Throwable {
                final String methodName = method.getName();
                if ("getBounds".equals(methodName)) {
                    return typeVar.getBounds();
                }
                if ("getGenericDeclaration".equals(methodName)) {
                    return typeVar.getGenericDeclaration();
                }
                if ("getName".equals(methodName)) {
                    return entityType.getSimpleName();
                }
                if ("getAnnotatedBounds".equals(methodName)) {
                    return method.invoke(typeVar, os);
                }
                if ("getAnnotation".equals(methodName)) {
                    return method.invoke(typeVar, os);
                }
                if ("getAnnotations".equals(methodName)) {
                    return method.invoke(typeVar, os);
                }
                if ("getDeclaredAnnotations".equals(methodName)) {
                    return method.invoke(typeVar, os);
                }
                return null;
            }
        }
}

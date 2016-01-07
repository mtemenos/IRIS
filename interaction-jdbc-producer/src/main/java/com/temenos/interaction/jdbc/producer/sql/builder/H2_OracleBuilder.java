package com.temenos.interaction.jdbc.producer.sql.builder;

/*
 * Utility class for building SQL commands.
 * 
 * If given a key constructs a command for a single row. 
 * 
 * If given a null key constructs a command to add all rows.
 */

/*
 * #%L
 * interaction-jdbc-producer
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

import com.temenos.interaction.authorization.command.data.AccessProfile;
import com.temenos.interaction.authorization.command.data.OrderBy;
import com.temenos.interaction.jdbc.ServerMode;
import com.temenos.interaction.jdbc.producer.sql.ColumnTypesMap;

public class H2_OracleBuilder extends OracleBuilder {

    /*
     * Constructor when there is a key.
     */
    public H2_OracleBuilder(String tableName, String keyValue, AccessProfile accessProfile,
            ColumnTypesMap colTypesMap, String top, String skip, List<OrderBy> orderBy) {
        super(tableName, keyValue, accessProfile, colTypesMap, top, skip, orderBy);
    }
    
    /*
     * Set up server compatibility modes.
     */
    @Override
    public void setCompatibilityMode() {
        this.serverMode = ServerMode.ORACLE;
        this.serverIsEmulated = true;
    }
}
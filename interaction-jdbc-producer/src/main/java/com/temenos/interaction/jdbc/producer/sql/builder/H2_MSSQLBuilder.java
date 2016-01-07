package com.temenos.interaction.jdbc.producer.sql.builder;

/*
 * #%L
 * interaction-jdbc-producer
 * %%
 * Copyright (C) 2012 - 2015 Temenos Holdings N.V.
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

/**
 * Implementation to build SQL Statement for MS SQL Server
 *
 * @author sjunejo
 *
 */
public class H2_MSSQLBuilder extends MSSQLBuilder {
    
    /**
     * Constructor to build a SQL Server Command Builder
     */
    public H2_MSSQLBuilder(String tableName, String keyValue, AccessProfile accessProfile, ColumnTypesMap colTypesMap,
            String top, String skip, List<OrderBy> orderBy) {
        super(tableName, keyValue, accessProfile, colTypesMap, top, skip, orderBy);
    }

    @Override
    public void setCompatibilityMode() {
        this.serverMode = ServerMode.MSSQL;
        this.serverIsEmulated = true;
    };
}
package com.temenos.interaction.jdbc.producer.sql;

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

import javax.ws.rs.core.Response.Status;

import com.temenos.interaction.authorization.command.data.AccessProfile;
import com.temenos.interaction.authorization.command.data.OrderBy;
import com.temenos.interaction.jdbc.ServerMode;
import com.temenos.interaction.jdbc.exceptions.JdbcException;
import com.temenos.interaction.jdbc.producer.sql.builder.H2_MSSQLBuilder;
import com.temenos.interaction.jdbc.producer.sql.builder.H2_OracleBuilder;
import com.temenos.interaction.jdbc.producer.sql.builder.MSSQLBuilder;
import com.temenos.interaction.jdbc.producer.sql.builder.OracleBuilder;

/**
 * Factory to return appropiate SqlBuilder implementation according to target server type in use 
 *
 * @author sjunejo
 *
 */
public class SqlBuilderFactory {

    public static SqlBuilder getSqlBuilder(String tableName, String keyValue, AccessProfile accessProfile, ColumnTypesMap colTypesMap, String top,
            String skip, List<OrderBy> orderBy, ServerMode serverMode) throws JdbcException {
        SqlBuilder builder = null;
        if (serverMode != null) {
            switch (serverMode) {
            case MSSQL:
                builder = new MSSQLBuilder(tableName, keyValue, accessProfile, colTypesMap, top, skip, orderBy);
                break;
            case H2_MSSQL:
                builder = new H2_MSSQLBuilder(tableName, keyValue, accessProfile, colTypesMap, top, skip, orderBy);
                break;
            case ORACLE:
                builder = new OracleBuilder(tableName, keyValue, accessProfile, colTypesMap, top, skip, orderBy);
                break;
            case H2_ORACLE:
                builder = new H2_OracleBuilder(tableName, keyValue, accessProfile, colTypesMap, top, skip, orderBy);
                break;
            default:
                throw new JdbcException(Status.PRECONDITION_FAILED, "DB Server \"" + serverMode.toString() + "\" not supported");
            }
        } else {
            // Defaults to Emulated MSSQL
            builder = new H2_MSSQLBuilder(tableName, keyValue, accessProfile, colTypesMap, top, skip, orderBy);
        }
        return builder;
    }
}

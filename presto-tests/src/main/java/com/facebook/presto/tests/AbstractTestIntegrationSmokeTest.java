/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.facebook.presto.tests;

import com.facebook.presto.testing.MaterializedResult;
import com.google.common.collect.ImmutableList;
import org.testng.annotations.Test;

import java.util.List;

import static com.facebook.presto.spi.type.VarcharType.VARCHAR;
import static com.facebook.presto.tests.QueryAssertions.assertContains;
import static org.testng.Assert.assertTrue;

public abstract class AbstractTestIntegrationSmokeTest
        extends AbstractTestQueryFramework
{
    protected AbstractTestIntegrationSmokeTest(QueryRunnerSupplier supplier)
    {
        super(supplier);
    }

    @Test
    public void testAggregateSingleColumn()
    {
        assertQuery("SELECT SUM(orderkey) FROM ORDERS");
        assertQuery("SELECT SUM(totalprice) FROM ORDERS");
        assertQuery("SELECT MAX(comment) FROM ORDERS");
    }

    @Test
    public void testColumnsInReverseOrder()
    {
        assertQuery("SELECT shippriority, clerk, totalprice FROM ORDERS");
    }

    @Test
    public void testCountAll()
    {
        assertQuery("SELECT COUNT(*) FROM ORDERS");
    }

    @Test
    public void testExactPredicate()
    {
        assertQuery("SELECT * FROM ORDERS WHERE orderkey = 10");
    }

    @Test
    public void testInListPredicate()
    {
        assertQuery("SELECT * FROM ORDERS WHERE orderkey IN (10, 11, 20, 21)");
    }

    @Test
    public void testIsNullPredicate()
    {
        assertQuery("SELECT * FROM ORDERS WHERE orderkey = 10 OR orderkey IS NULL");
    }

    @Test
    public void testMultipleRangesPredicate()
    {
        assertQuery("SELECT * FROM ORDERS WHERE orderkey BETWEEN 10 AND 50 or orderkey BETWEEN 100 AND 150");
    }

    @Test
    public void testRangePredicate()
    {
        assertQuery("SELECT * FROM ORDERS WHERE orderkey BETWEEN 10 AND 50");
    }

    @Test
    public void testSelectAll()
    {
        assertQuery("SELECT * FROM ORDERS");
    }

    @Test
    public void testShowSchemas()
    {
        MaterializedResult actualSchemas = computeActual("SHOW SCHEMAS").toTestTypes();

        MaterializedResult.Builder resultBuilder = MaterializedResult.resultBuilder(getQueryRunner().getDefaultSession(), VARCHAR)
                .row(getQueryRunner().getDefaultSession().getSchema().orElse("tpch"));

        assertContains(actualSchemas, resultBuilder.build());
    }

    @Test
    public void testShowTables()
    {
        MaterializedResult actualTables = computeActual("SHOW TABLES").toTestTypes();
        MaterializedResult expectedTables = MaterializedResult.resultBuilder(getQueryRunner().getDefaultSession(), VARCHAR)
                .row("orders")
                .build();
        assertContains(actualTables, expectedTables);
    }

    @Test
    public void testDescribeTable()
    {
        MaterializedResult actualColumns = computeActual("DESC ORDERS").toTestTypes();

        // some connectors don't support dates, and some do not support parametrized varchars, so we check multiple options
        List<MaterializedResult> expectedColumnsPossibilities = ImmutableList.of(
                getExpectedTableDescription(true, true),
                getExpectedTableDescription(true, false),
                getExpectedTableDescription(false, true),
                getExpectedTableDescription(false, false));
        assertTrue(expectedColumnsPossibilities.contains(actualColumns), String.format("%s not in %s", actualColumns, expectedColumnsPossibilities));
    }

    @Test
    public void testDuplicatedRowCreateTable()
    {
        assertQueryFails("CREATE TABLE test (a integer, a integer)",
                "line 1:31: Column name 'a' specified more than once");
        assertQueryFails("CREATE TABLE test (a integer, orderkey integer, LIKE orders INCLUDING PROPERTIES)",
                "line 1:49: Column name 'orderkey' specified more than once");

        assertQueryFails("CREATE TABLE test (a integer, A integer)",
                "line 1:31: Column name 'A' specified more than once");
        assertQueryFails("CREATE TABLE test (a integer, OrderKey integer, LIKE orders INCLUDING PROPERTIES)",
                "line 1:49: Column name 'orderkey' specified more than once");
    }

    private MaterializedResult getExpectedTableDescription(boolean dateSupported, boolean parametrizedVarchar)
    {
        String orderDateType;
        if (dateSupported) {
            orderDateType = "date";
        }
        else {
            orderDateType = "varchar";
        }
        if (parametrizedVarchar) {
            return MaterializedResult.resultBuilder(getQueryRunner().getDefaultSession(), VARCHAR, VARCHAR, VARCHAR, VARCHAR)
                    .row("orderkey", "bigint", "", "")
                    .row("custkey", "bigint", "", "")
                    .row("orderstatus", "varchar", "", "")
                    .row("totalprice", "double", "", "")
                    .row("orderdate", orderDateType, "", "")
                    .row("orderpriority", "varchar", "", "")
                    .row("clerk", "varchar", "", "")
                    .row("shippriority", "integer", "", "")
                    .row("comment", "varchar", "", "")
                    .build();
        }
        else {
            return MaterializedResult.resultBuilder(getQueryRunner().getDefaultSession(), VARCHAR, VARCHAR, VARCHAR, VARCHAR)
                    .row("orderkey", "bigint", "", "")
                    .row("custkey", "bigint", "", "")
                    .row("orderstatus", "varchar(1)", "", "")
                    .row("totalprice", "double", "", "")
                    .row("orderdate", orderDateType, "", "")
                    .row("orderpriority", "varchar(15)", "", "")
                    .row("clerk", "varchar(15)", "", "")
                    .row("shippriority", "integer", "", "")
                    .row("comment", "varchar(79)", "", "")
                    .build();
        }
    }
}

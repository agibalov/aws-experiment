package io.agibalov;

import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static org.junit.Assert.assertEquals;

public class DummyTest {
    @Test
    public void dummy() {
        String host = System.getenv("REDSHIFT_HOST");
        String port = System.getenv("REDSHIFT_PORT");
        String database = System.getenv("REDSHIFT_DATABASE");
        String username = System.getenv("REDSHIFT_USERNAME");
        String password = System.getenv("REDSHIFT_PASSWORD");
        String importCsvFileS3Url = System.getenv("IMPORT_CSV_FILE_S3_URL");
        String exportCsvS3Prefix = System.getenv("EXPORT_CSV_S3_PREFIX");
        String iamRoleArn = System.getenv("IMPORT_IAM_ROLE_ARN");
        String region = System.getenv("IMPORT_REGION");

        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                String.format("jdbc:redshift://%s:%s/%s", host, port, database),
                username,
                password);
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute("drop table if exists employees");
        jdbcTemplate.execute("create table employees(id varchar primary key, salary integer not null)");

        jdbcTemplate.execute(String.format(
                "copy employees(id, salary) from '%s' iam_role '%s' region '%s' csv",
                importCsvFileS3Url, iamRoleArn, region));
        int averageSalary = jdbcTemplate.queryForObject("select avg(salary) from employees", Integer.class);
        assertEquals(136, averageSalary);

        jdbcTemplate.execute(String.format(
                "unload ('select avg(salary) from employees') " +
                        "to '%s' iam_role '%s' region '%s' " +
                        "manifest header csv allowoverwrite",
                exportCsvS3Prefix, iamRoleArn, region));
    }
}

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.xream.x7;

import io.xream.x7.cache.DefaultL2CacheResolver;
import io.xream.x7.repository.dao.TxConfig;
import io.xream.x7.repository.id.DefaultIdGeneratorService;
import io.xream.x7.repository.id.IdGeneratorService;
import io.xream.x7.repository.internal.DomainObjectRepositoy;
import io.xream.x7.repository.mapper.Dialect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import io.xream.x7.common.config.ConfigAdapter;
import io.xream.x7.common.repository.CacheResolver;
import io.xream.x7.common.util.StringUtil;
import io.xream.x7.repository.CacheableRepository;
import io.xream.x7.repository.CriteriaParser;
import io.xream.x7.repository.DbType;
import io.xream.x7.repository.Repository;
import io.xream.x7.repository.dao.Dao;
import io.xream.x7.repository.dao.DaoImpl;
import io.xream.x7.repository.dao.SqlCriteriaParser;
import io.xream.x7.repository.mapper.MapperFactory;
import io.xream.x7.repository.transform.DataTransform;
import io.xream.x7.repository.transform.SqlDataTransform;

import java.util.Objects;


public class RepositoryStarter  {

    private Logger logger = LoggerFactory.getLogger(RepositoryStarter.class);


    @Bean
    @Order(2)
    public Dialect dialect(Environment environment){
        String driverClassName = environment.getProperty("spring.datasource.driver-class-name");

        String driver = null;
        if (Objects.isNull(driverClassName)) {
            driver = environment.getProperty("x7.db.driver");
        } else {
            driver = driverClassName;
        }

        driver = driver.toLowerCase();
        Dialect dialect = null;
        try {
            if (driver.contains(DbType.MYSQL)) {
                DbType.value = DbType.MYSQL;
                dialect = (Dialect) Class.forName("io.xream.x7.repository.dialect.MySqlDialect").newInstance();
            } else if (driver.contains(DbType.ORACLE)) {
                DbType.value = DbType.ORACLE;
                dialect = (Dialect) Class.forName("io.xream.x7.repository.dialect.OracleDialect").newInstance();
            }
            initDialect(dialect);
        }catch (Exception e){

        }

        return dialect;
    }

    @Bean
    @Order(3)
    public CriteriaParser criteriaParser(Dialect dialect,Environment environment) {

        String driverClassName = environment.getProperty("spring.datasource.driver-class-name");

        CriteriaParser criteriaParser =  null;
        if (driverClassName.toLowerCase().contains("mysql")
                || driverClassName.toLowerCase().contains("oracle")) {
            criteriaParser = new SqlCriteriaParser();
            criteriaParser.setDialect(dialect);
        }

        return criteriaParser;
    }


    @Bean
    @Order(4)
    public Dao dao(Environment environment){

        String driverClassName = environment.getProperty("spring.datasource.driver-class-name");
        Dao dao =  null;
        if (driverClassName.toLowerCase().contains("mysql")
                || driverClassName.toLowerCase().contains("oracle")) {
            dao = new DaoImpl();
        }
        return dao;
    }

    @Bean
    @Order(5)
    public CacheResolver cacheResolver(){
        return new DefaultL2CacheResolver();
    }


    @Bean
    @Order(6)
    public IdGeneratorService idGenerator(){
        DefaultIdGeneratorService idGenerator = new DefaultIdGeneratorService();
        return idGenerator;
    }

    @Bean
    @Order(7)
    public Repository dataRepository(Dao dao, CacheResolver cacheResolver,Environment environment){

        String driverClassName = environment.getProperty("spring.datasource.driver-class-name");

        DataTransform dataTransform = null;

        if (driverClassName.toLowerCase().contains("mysql")
                || driverClassName.toLowerCase().contains("oracle")) {
            dataTransform = new SqlDataTransform();
            ((SqlDataTransform) dataTransform).setDao(dao);
        }

        CacheableRepository repository = new CacheableRepository();
        repository.setDataTransform(dataTransform);
        repository.setCacheResolver(cacheResolver);

        return repository;
    }


    @Bean
    @Order(9)
    public DomainObjectRepositoy domainObjectRepositoy(Repository repository) {
        DomainObjectRepositoy domainObjectRepositoy = new DomainObjectRepositoy();
        domainObjectRepositoy.setRepository(repository);
        return domainObjectRepositoy;
    }

    @ConditionalOnMissingBean(X7Data.class)
    @Bean
    @Order(10)
    public X7Data enableData(Environment env){


        {
            String showSqlStr = env.getProperty("x7.repository.show-sql");
            boolean showSql = false;
            if (StringUtil.isNotNull(showSqlStr)){
                try {
                    showSql = Boolean.parseBoolean(showSqlStr);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
            if (showSql) {
                ConfigAdapter.setIsShowSql(true);
            }else{
                logger.info("X7 Repsository will not show SQL, for no config like one of: x7.repository.show-sql=true" );
            }

        }

        return new X7Data();
    }

    @ConditionalOnMissingBean(TxConfig.class)
    @Bean
    @Order(11)
    public TxConfig txConfig(DataSourceTransactionManager dstm){
        return new TxConfig(dstm);
    }


    /**
     * TODO:
     *      改成Map,可以动态获取方言
     * @param dialect
     */
    private void initDialect(Dialect dialect) {
        MapperFactory.Dialect = dialect;
    }

}
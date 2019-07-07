package com.config;

import javax.sql.DataSource;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.LineMapper;
import org.springframework.batch.item.file.MultiResourceItemReader;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import com.config.condition.H2Condition;
import com.config.condition.MySQLCondition;
import com.listner.ItemCountListener;
import com.model.Employee;
import com.task.FileDeletingTasklet;
import com.validation.ValidationProcessor;

@Configuration
@EnableBatchProcessing
public class BatchConfig {

	@Autowired
	private JobBuilderFactory jobBuilderFactory;

	@Autowired
	private StepBuilderFactory stepBuilderFactory;

	//@Value("classPath:/input/inputData.csv")
	//private Resource inputResource;
	
	@Value("file:C:/Users/sreekanth/Desktop/t/inputData*.csv")
	private Resource[] inputResources;

	@Value("${domain.datasource.type}")
	private String type;

	@Bean
	public Job readCSVFileJob() {
		return jobBuilderFactory
				.get("readCSVFileJob")
				.incrementer(new RunIdIncrementer())
				.start(step1())
				.next(step2())
				.build();
	}

	 @Bean
	 public Step step1() {
	        return stepBuilderFactory.get("step1").<Employee, Employee>chunk(5)
	                .reader(multiResourceItemReader())
	                .processor(validationProcessor())
	                .writer(writer())
	                .listener(listner())
	                .build();
	    }
	 
	 @Bean
	 public Step step2() {
	        FileDeletingTasklet task = new FileDeletingTasklet();
	        task.setResources(inputResources);
	        return stepBuilderFactory.get("step2")
	                .tasklet(task)
	                .build();
	    }
	 
	@Bean
    public MultiResourceItemReader<Employee> multiResourceItemReader() {
        MultiResourceItemReader<Employee> resourceItemReader = new MultiResourceItemReader<Employee>();
        resourceItemReader.setResources(inputResources);
        resourceItemReader.setDelegate(reader());
        return resourceItemReader;
    }
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
    @Bean
    public FlatFileItemReader<Employee> reader()  {
        // Create reader instance
        FlatFileItemReader<Employee> reader = new FlatFileItemReader<Employee>();
        // Set number of lines to skips. Use it if file has header rows.
        reader.setLinesToSkip(1);
        // Configure how each line will be parsed and mapped to different values
        reader.setLineMapper(new DefaultLineMapper() {
            {
                // 3 columns in each row
                setLineTokenizer(new DelimitedLineTokenizer() {
                    {
                        setNames(new String[] { "id", "firstName", "lastName" });
                    }
                });
                // Set values in Employee class
                setFieldSetMapper(new BeanWrapperFieldSetMapper<Employee>() {
                    {
                        setTargetType(Employee.class);
                    }
                });
            }
        });
        return reader;
    }
	@Bean
	public ItemCountListener listner() {
		return new ItemCountListener();
	}

	@Bean
	public ItemProcessor<Employee, Employee> processor() {
		return new DBLogProcessor();
	}

	@Bean
	public LineMapper<Employee> lineMapper() {
		DefaultLineMapper<Employee> lineMapper = new DefaultLineMapper<Employee>();
		DelimitedLineTokenizer lineTokenizer = new DelimitedLineTokenizer();
		lineTokenizer.setNames(new String[] { "id", "firstName", "lastName" });
		lineTokenizer.setIncludedFields(new int[] { 0, 1, 2 });
		BeanWrapperFieldSetMapper<Employee> fieldSetMapper = new BeanWrapperFieldSetMapper<Employee>();
		fieldSetMapper.setTargetType(Employee.class);
		lineMapper.setLineTokenizer(lineTokenizer);
		lineMapper.setFieldSetMapper(fieldSetMapper);
		return lineMapper;
	}

	@Bean
	public JdbcBatchItemWriter<Employee> writer() {
		JdbcBatchItemWriter<Employee> itemWriter = new JdbcBatchItemWriter<Employee>();
		if (type.equals("MYSQL")) {
			itemWriter.setDataSource(dataSourceMysql());
		} else if (type.equals("H2")) {
			itemWriter.setDataSource(dataSourceH2());
		}
		itemWriter.setSql("INSERT INTO EMPLOYEE (ID, FIRSTNAME, LASTNAME) VALUES (:id, :firstName, :lastName)");
		itemWriter.setItemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<Employee>());
		return itemWriter;
	}

	@Bean
	@Conditional(H2Condition.class)
	public DataSource dataSourceH2() {
		EmbeddedDatabaseBuilder embeddedDatabaseBuilder = new EmbeddedDatabaseBuilder();
		return embeddedDatabaseBuilder.addScript("classpath:org/springframework/batch/core/schema-drop-h2.sql")
				.addScript("classpath:org/springframework/batch/core/schema-h2.sql")
				// .addScript("classpath:jdbc/schema.sql")
				.addScript("classpath:employeeH2.sql").setType(EmbeddedDatabaseType.H2).build();
	}


	@Bean
	@Conditional(MySQLCondition.class)
	public DataSource dataSourceMysql() {
		return DataSourceBuilder.create().username("root").password("root")
				.url("jdbc:mysql://localhost:3306/mydb?createDatabaseIfNotExist=true")
				.driverClassName("com.mysql.jdbc.Driver").build();
	}

	@Bean
	@Conditional(MySQLCondition.class)
	public DataSourceInitializer dataSourceInitializer(DataSource dataSource) {
		DataSourceInitializer dataSourceInitializer = new DataSourceInitializer();
		dataSourceInitializer.setDataSource(dataSource);
		ResourceDatabasePopulator databasePopulator = new ResourceDatabasePopulator();
		databasePopulator.addScript(new ClassPathResource("org/springframework/batch/core/schema-drop-mysql.sql"));
		databasePopulator.addScript(new ClassPathResource("org/springframework/batch/core/schema-mysql.sql"));
		databasePopulator.addScript(new ClassPathResource("employeeMysql.sql"));
		dataSourceInitializer.setDatabasePopulator(databasePopulator);
		// dataSourceInitializer.setEnabled(Boolean.parseBoolean(initDatabase));
		return dataSourceInitializer;
	}

	 @Bean
	 public ItemProcessor<Employee, Employee> validationProcessor() {
	        return new ValidationProcessor();
	    }
}
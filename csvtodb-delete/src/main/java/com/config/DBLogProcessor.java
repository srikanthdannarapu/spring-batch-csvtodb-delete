package com.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;

import com.model.Employee;

public class DBLogProcessor implements ItemProcessor<Employee, Employee> {
	private static final Logger log = LoggerFactory.getLogger(DBLogProcessor.class);
	public Employee process(Employee employee) throws Exception {
		log.info("Inserting employee : {} " , employee);
		return employee;
	}
}
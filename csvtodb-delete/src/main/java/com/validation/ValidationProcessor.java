package com.validation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;

import com.model.Employee;

public class ValidationProcessor implements ItemProcessor<Employee, Employee> {
	private static final Logger log = LoggerFactory.getLogger(ValidationProcessor.class);
	public Employee process(Employee employee) throws Exception {
		if (employee.getId() == null) {
			log.info("Missing employee id : {}", employee.getId());
			return null;
		}

		try {
			if (Integer.valueOf(employee.getId()) <= 0) {
				log.info("Invalid employee id : {}",  employee.getId());
				return null;
			}
		} catch (NumberFormatException e) {
			log.info("Invalid employee id : {}", employee.getId());
			return null;
		}
		if (employee.getFirstName().length() > 12) {
			log.info("employee name too long: {}", employee.getFirstName());
			return null;
		}
		return employee;
	}
}
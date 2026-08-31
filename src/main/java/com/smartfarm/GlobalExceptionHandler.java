package com.smartfarm;

import java.sql.SQLException;
import java.time.Instant;

import org.hibernate.TransientPropertyValueException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
 
import jakarta.persistence.EntityNotFoundException;

@ControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiResponse<String>> handleJsonError(HttpMessageNotReadableException ex){
		return ResponseEntity.status(400).body(new ApiResponse<>(null, "Failed to save! \nCheck all fields and try again", false,Instant.now())); 
	}
	
	@ExceptionHandler(SQLException.class)
	public ResponseEntity<String> handleJsonError(SQLException ex){ 
		return ResponseEntity.status(400).body("Database error !"); 
	}
	
	@ExceptionHandler(MethodArgumentNotValidException.class) 
	public ResponseEntity<ApiResponse<String>> handleInvalidData(MethodArgumentNotValidException ex){ 
		String message = ex.getBindingResult().getFieldError().getDefaultMessage();
		return ResponseEntity.status(400).body(new ApiResponse<>(null, message, false,Instant.now()));   
	}
	@ExceptionHandler(NullPointerException.class)
	public ResponseEntity<ApiResponse<String>> handleJsonError(NullPointerException ex){ 
		return ResponseEntity.status(400).body(new ApiResponse<>(null, "Missing field! \nCheck all fields and try again", false,Instant.now()));  
	}
	
	@ExceptionHandler(EntityNotFoundException.class) 
	public ResponseEntity<ApiResponse<String>> notFound(EntityNotFoundException ex){ 
		
		return ResponseEntity.status(404).body(new ApiResponse<>(null, ex.getMessage(), false,Instant.now()));   
	}
	
	@ExceptionHandler(IllegalArgumentException.class)  
	public ResponseEntity<ApiResponse<String>> badRequest(IllegalArgumentException ex){ 
		
		return ResponseEntity.status(400).body(new ApiResponse<>(null, ex.getMessage(), false,Instant.now())); 
	}
	
	@ExceptionHandler(TransientPropertyValueException.class)  
	public ResponseEntity<ApiResponse<String>> foreign(TransientPropertyValueException ex){  
		
		return ResponseEntity.status(400).body(new ApiResponse<>(null, ex.getMessage(), false,Instant.now())); 
	}
	
	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ApiResponse<String>> dataIngegrityViolation(DataIntegrityViolationException ex){  
		
		return ResponseEntity.status(400).body(new ApiResponse<>(null, "Name must be unique!", false,Instant.now())); 
	}
}

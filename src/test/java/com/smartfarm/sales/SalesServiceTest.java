package com.smartfarm.sales;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.smartfarm.ApiResponse;
import com.smartfarm.customers.Customer;
import com.smartfarm.customers.CustomerRepository;
import com.smartfarm.customers.CustomerRequest;
import com.smartfarm.customers.CustomerService;
import com.smartfarm.projects.Project;
import com.smartfarm.projects.ProjectRepository;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class SalesServiceTest {

	@Mock
	private SalesRepository salesRepo;

	@Mock
	private ProjectRepository projectRepo;

	@Mock
	private CustomerRepository customerRepo;

	@Mock
	private CustomerService customerService;

	@InjectMocks
	private SalesService salesService;

	private Project mockProject;

	@BeforeEach
	void setUp() {
		mockProject = new Project();
		mockProject.setId("P001");
		mockProject.setName("Tomato Greenhouse");
	}

	@Test
	void createSale_withNewCustomer_successfullyCreatesSaleAndCustomer() {
		when(projectRepo.findById("P001")).thenReturn(Optional.of(mockProject));
		when(salesRepo.count()).thenReturn(0L);
		when(salesRepo.existsById(anyString())).thenReturn(false);

		CustomerRequest custReq = new CustomerRequest("School cafeteria", "0784463737", "12345678", "Kitale", "new");
		Customer savedCustomer = new Customer("C001", "School cafeteria", "0784463737", "12345678", "Kitale", true);

		when(customerRepo.existsByContact("0784463737")).thenReturn(false);
		when(customerRepo.existsByIdNumber("12345678")).thenReturn(false);
		when(customerService.saveCustomer(custReq)).thenReturn(savedCustomer);
		when(salesRepo.save(any(Sale.class))).thenAnswer(invocation -> invocation.getArgument(0));

		CreateSaleRequest request = new CreateSaleRequest("Milk", 20.0f, new BigDecimal("75.00"), "P001", custReq);
		ResponseEntity<ApiResponse<Sale>> response = salesService.createSale(request);

		assertEquals(HttpStatus.CREATED, response.getStatusCode());
		assertTrue(response.getBody().success());
		Sale sale = response.getBody().body();
		assertNotNull(sale);
		assertEquals("Milk", sale.getItem());
		assertEquals(20.0f, sale.getQuantity());
		assertEquals(new BigDecimal("75.00"), sale.getUnit_price());
		assertEquals(0, new BigDecimal("1500.00").compareTo(sale.getTotal_amount()));
		assertEquals(mockProject, sale.getProject());
		assertEquals("P001", sale.getProjectId());
		assertEquals(savedCustomer, sale.getCustomer());
		verify(customerService).saveCustomer(custReq);
		verify(salesRepo).save(any(Sale.class));
	}

	@Test
	void createSale_withDuplicateCustomerContact_returnsBadRequest() {
		when(projectRepo.findById("P001")).thenReturn(Optional.of(mockProject));
		CustomerRequest custReq = new CustomerRequest("School cafeteria", "0784463737", "12345678", "Kitale", "new");
		when(customerRepo.existsByContact("0784463737")).thenReturn(true);

		CreateSaleRequest request = new CreateSaleRequest("Milk", 20.0f, new BigDecimal("75.00"), "P001", custReq);
		ResponseEntity<ApiResponse<Sale>> response = salesService.createSale(request);

		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
		assertFalse(response.getBody().success());
		assertTrue(response.getBody().message().contains("0784463737"));
		verify(customerService, never()).saveCustomer(any());
		verify(salesRepo, never()).save(any());
	}

	@Test
	void createSale_withExistingCustomer_attachesFoundCustomer() {
		when(projectRepo.findById("P001")).thenReturn(Optional.of(mockProject));
		when(salesRepo.count()).thenReturn(5L);
		when(salesRepo.existsById(anyString())).thenReturn(false);

		Customer existingCustomer = new Customer("C001", "School cafeteria", "0784463737", "12345678", "Kitale", true);
		CustomerRequest custReq = new CustomerRequest("School cafeteria", "0784463737", "12345678", "Kitale", "exist");
		when(customerRepo.findByContact("0784463737")).thenReturn(Optional.of(existingCustomer));
		when(salesRepo.save(any(Sale.class))).thenAnswer(invocation -> invocation.getArgument(0));

		CreateSaleRequest request = new CreateSaleRequest("Eggs", 10.0f, new BigDecimal("400.00"), "P001", custReq);
		ResponseEntity<ApiResponse<Sale>> response = salesService.createSale(request);

		assertEquals(HttpStatus.CREATED, response.getStatusCode());
		assertTrue(response.getBody().success());
		assertEquals(existingCustomer, response.getBody().body().getCustomer());
		verify(customerService, never()).saveCustomer(any());
	}

	@Test
	void createSale_withoutCustomer_successfullyCreatesSale() {
		when(projectRepo.findById("P001")).thenReturn(Optional.of(mockProject));
		when(salesRepo.count()).thenReturn(1L);
		when(salesRepo.existsById(anyString())).thenReturn(false);
		when(salesRepo.save(any(Sale.class))).thenAnswer(invocation -> invocation.getArgument(0));

		CreateSaleRequest request = new CreateSaleRequest("Tomatoes", 50.0f, new BigDecimal("80.00"), "P001", null);
		ResponseEntity<ApiResponse<Sale>> response = salesService.createSale(request);

		assertEquals(HttpStatus.CREATED, response.getStatusCode());
		assertTrue(response.getBody().success());
		assertNull(response.getBody().body().getCustomer());
	}

	@Test
	void createSale_whenProjectNotFound_throwsEntityNotFoundException() {
		when(projectRepo.findById("UNKNOWN")).thenReturn(Optional.empty());

		CreateSaleRequest request = new CreateSaleRequest("Tomatoes", 10.0f, new BigDecimal("80.00"), "UNKNOWN", null);
		assertThrows(EntityNotFoundException.class, () -> salesService.createSale(request));
	}

	@Test
	void getSalesByProjectId_returnsList() {
		Sale s1 = new Sale("S001", "Milk", 10, new BigDecimal("70"), null, new BigDecimal("700"), mockProject, null);
		org.springframework.data.domain.Page<Sale> page = new org.springframework.data.domain.PageImpl<>(List.of(s1));
		when(salesRepo.findByProjectId("P001", org.springframework.data.domain.PageRequest.of(0, 10))).thenReturn(page);

		ResponseEntity<ApiResponse<org.springframework.data.domain.Page<Sale>>> response = salesService.getSalesByProjectId("P001", 0, 10);
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals(1, response.getBody().body().getContent().size());
	}

	@Test
	void deleteSale_whenExists_deletesSale() {
		when(salesRepo.existsById("S001")).thenReturn(true);

		ResponseEntity<ApiResponse<Void>> response = salesService.deleteSale("S001");
		assertEquals(HttpStatus.OK, response.getStatusCode());
		verify(salesRepo).deleteById("S001");
	}
}

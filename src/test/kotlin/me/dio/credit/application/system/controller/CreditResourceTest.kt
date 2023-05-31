package me.dio.credit.application.system.controller

import com.fasterxml.jackson.databind.ObjectMapper
import me.dio.credit.application.system.dto.request.CreditDto
import me.dio.credit.application.system.dto.request.CustomerDto
import me.dio.credit.application.system.entity.Address
import me.dio.credit.application.system.entity.Customer
import me.dio.credit.application.system.repository.CreditRepository
import me.dio.credit.application.system.repository.CreditRepositoryTest
import me.dio.credit.application.system.repository.CustomerRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.math.BigDecimal
import java.time.LocalDate

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@ContextConfiguration
class CreditResourceTest {

    @Autowired
    private lateinit var creditRepository: CreditRepository

    @Autowired
    private lateinit var customerRepository: CustomerRepository

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    companion object {
        const val URL: String = "/api/credits"
    }

    @BeforeEach
    fun setup() {
        creditRepository.deleteAll()
        customerRepository.deleteAll()
    }

    @AfterEach
    fun tearDown() {
        creditRepository.deleteAll()
        customerRepository.deleteAll()
    }

    @Test
    fun `should create a credit and return 201 status`() {
        //given
        val customer: Customer = customerRepository.save(builderCustomerDto().toEntity())

        val creditDto: CreditDto = builderCreditDto(customerId = customer.id!!)
        val valueAsString: String = objectMapper.writeValueAsString(creditDto)

        //when
        //then
        mockMvc.perform(
            MockMvcRequestBuilders.post(URL)
            .contentType(MediaType.APPLICATION_JSON)
            .content(valueAsString)
        )
            .andExpect(MockMvcResultMatchers.status().isCreated)
            .andExpect(MockMvcResultMatchers.jsonPath("$.creditValue").value(1000.0))
            .andExpect(MockMvcResultMatchers.jsonPath("$.numberOfInstallment").value(10))
            .andExpect(MockMvcResultMatchers.jsonPath("$.emailCustomer").value("felipe@teste.com"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.incomeCustomer").value(3000.0))
            .andDo(MockMvcResultHandlers.print())
    }

    @Test
    fun `should not save a credit with numberOfInstallment greater than 48 installments and return 400 status`() {
        //given
        customerRepository.save(builderCustomerDto().toEntity())

        val creditDto: CreditDto = builderCreditDto(numberOfInstallments = 50)
        val valueAsString: String = objectMapper.writeValueAsString(creditDto)

        //when
        //then
        mockMvc.perform(
            MockMvcRequestBuilders.post(URL)
            .contentType(MediaType.APPLICATION_JSON)
            .content(valueAsString)
        )
        .andExpect(MockMvcResultMatchers.status().isBadRequest)
        .andExpect(MockMvcResultMatchers.jsonPath("$.title").value("Bad Request! Consult the documentation"))
        .andExpect(MockMvcResultMatchers.jsonPath("$.timestamp").exists())
        .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(400))
        .andExpect(
            MockMvcResultMatchers.jsonPath("$.exception")
            .value("class org.springframework.web.bind.MethodArgumentNotValidException")
        )
        .andExpect(MockMvcResultMatchers.jsonPath("$.details[*]").isNotEmpty)
        .andDo(MockMvcResultHandlers.print())
    }

    @Test
    fun `should find list of credits by customer id and return 200 status`() {

        //given
        val customer: Customer = customerRepository.save(builderCustomerDto().toEntity())
        creditRepository.save(builderCreditDto(customerId = customer.id!!, numberOfInstallments = 10).toEntity())
        creditRepository.save(builderCreditDto(customerId = customer.id!!, numberOfInstallments = 5).toEntity())

        //when
        //then
        mockMvc.perform(
            MockMvcRequestBuilders.get("${CreditResourceTest.URL}")
            .accept(MediaType.APPLICATION_JSON)
            .param("customerId", customer.id.toString())
        )
        .andExpect(MockMvcResultMatchers.status().isOk)
        .andExpect(MockMvcResultMatchers.jsonPath("$.[*]").exists())
        .andDo(MockMvcResultHandlers.print())
    }

    private fun builderCreditDto(
            creditValue: BigDecimal = BigDecimal.valueOf(1000.0),
            dayFirstOfInstallment: LocalDate = LocalDate.now().plusMonths(2L),
            numberOfInstallments: Int = 10,
            customerId: Long = 1L
    ) = CreditDto(
            creditValue = creditValue,
            dayFirstOfInstallment = dayFirstOfInstallment,
            numberOfInstallments = numberOfInstallments,
            customerId = customerId
    )

    private fun builderCustomerDto(
            firstName: String = "Felipe",
            lastName: String = "Fruhauf",
            cpf: String = "12345678910",
            email: String = "felipe@teste.com",
            income: BigDecimal = BigDecimal.valueOf(3000.0),
            password: String = "123456",
            zipCode: String = "99555000",
            street: String = "Rua dos Testes",
    ) = CustomerDto(
            firstName = firstName,
            lastName = lastName,
            cpf = cpf,
            email = email,
            income = income,
            password = password,
            zipCode = zipCode,
            street = street
    )
}
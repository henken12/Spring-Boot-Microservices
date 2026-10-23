package com.eazybytes.accounts.service.impl;

import com.eazybytes.accounts.constants.AccountsConstants;
import com.eazybytes.accounts.dto.AccountsDto;
import com.eazybytes.accounts.dto.AccountsMsgDto;
import com.eazybytes.accounts.dto.CustomerDto;
import com.eazybytes.accounts.entity.Account;
import com.eazybytes.accounts.entity.Customer;
import com.eazybytes.accounts.exception.CustomerAlreadyExistsException;
import com.eazybytes.accounts.exception.ResourceNotFoundException;
import com.eazybytes.accounts.mapper.AccountMapper;
import com.eazybytes.accounts.mapper.CustomerMapper;
import com.eazybytes.accounts.repository.AccountRepository;
import com.eazybytes.accounts.repository.CustomerRepositery;
import com.eazybytes.accounts.service.IAccountsService;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Service
@AllArgsConstructor
public class AccountsServiceImpl implements IAccountsService {

    private static final Logger log = LoggerFactory.getLogger(AccountsServiceImpl.class);

    private AccountRepository accountRepository;
    @Autowired
    private CustomerRepositery customerRepositery;
    private final StreamBridge streamBridge;

    @Override
    public void createAccount(CustomerDto customerDto) {
        Customer customer = CustomerMapper.mapToCustomer(customerDto,new Customer());
        Optional<Customer> optionalCustomer = customerRepositery.findByMobileNumber(customerDto.getMobileNumber());
        if(optionalCustomer.isPresent()){
            throw new CustomerAlreadyExistsException("customer already registered with a given mobileNumber"+
                    customerDto.getMobileNumber());
        }
        Customer savedCustomer = customerRepositery.save(customer);
        Account savedAccount = accountRepository.save(createdNewAccount(savedCustomer));
        sendCommunicaton(savedAccount, savedCustomer);
    }
    private void sendCommunicaton(Account account, Customer customer){
        var accountsMsgDto = new AccountsMsgDto(account.getAccountNumber(), customer.getName(), customer.getEmail(), customer.getMobileNumber());
        log.info("sending communication request for the details: {}",accountsMsgDto);
        var result = streamBridge.send("sendCommunication-out-0",accountsMsgDto);
        log.info("is the communication request successfully triggered?: {}",result);
    }

    /**
     *
     * @param customer - customer object
     * @return the new account details
     */

    private Account createdNewAccount(Customer customer){
        Account newAccount = new Account();
        newAccount.setCustomerId(customer.getCustomerId());
        long randomAccNumber = 1000000000L + new Random().nextInt(900000000);

        newAccount.setAccountNumber(randomAccNumber);
        newAccount.setAccountType(AccountsConstants.SAVINGS);
        newAccount.setBranchAddress(AccountsConstants.ADDRESS);
        return newAccount;
    }

    /**
     *
     * @param mobileNumber-input Mobile Number
     * @return Accounts Details based on a given mobileNumber
     */
    @Override
    public CustomerDto fetchAccount(String mobileNumber) {
        Customer customer = customerRepositery.findByMobileNumber(mobileNumber).orElseThrow(
                ()->new ResourceNotFoundException("customer","mobileNumber",mobileNumber)
        );
        Account account = accountRepository.findByCustomerId(customer.getCustomerId()).orElseThrow(
                ()->new ResourceNotFoundException("Account","customerId",customer.getCustomerId().toString())
        );

        CustomerDto customerDto = CustomerMapper.mapToCustomerDto(customer, new CustomerDto());
        customerDto.setAccountsDto(AccountMapper.mapToAccountDto(account,new AccountsDto()));
        return customerDto;
    }
    /**
     *
     * @param customerDto- CustomerDto Object
     * @return boolean indicating if the update of Account details is successful or not
     */
    @Override
    public boolean updateAccount(CustomerDto customerDto) {
       boolean isUpdated= false;
       AccountsDto accountsDto = customerDto.getAccountsDto();
       if(accountsDto != null){
           Account account = accountRepository.findById(accountsDto.getAccountNumber()).orElseThrow(
                   ()->new ResourceNotFoundException("Account","AccountNumber",accountsDto.getAccountNumber().toString())
           );
           AccountMapper.mapToAccounts(accountsDto,account);
           account = accountRepository.save(account);

           Long customerId = account.getCustomerId();
           Customer customer = customerRepositery.findById(customerId).orElseThrow(
                   ()->new ResourceNotFoundException("customer","customerId",customerId.toString())
           );
           CustomerMapper.mapToCustomer(customerDto,customer);
           customerRepositery.save(customer);
           isUpdated = true;
       }
       return isUpdated;
    }
    /**
     *
     * @param mobileNumber-Input mobile number
     * @return boolean indicating if the delete of Account details is successful or not
     */
    @Override
    public boolean deleteAccount(String mobileNumber) {
        Customer customer = customerRepositery.findByMobileNumber(mobileNumber).orElseThrow(
                ()->new ResourceNotFoundException("Customer","mobileNumber",mobileNumber)
        );
        accountRepository.deleteByCustomerId(customer.getCustomerId());
        customerRepositery.deleteById(customer.getCustomerId());
        return true;
    }

    @Override
    public boolean updateCommunicationStatus(Long accountNumber) {
        boolean isUpdated = false;
        if(accountNumber!=null){
            Account account = accountRepository.findById(accountNumber).orElseThrow(
                    ()->new ResourceNotFoundException("Account","AccountNumber",accountNumber.toString())
            );
            account.setCommunicationSw(true);
            accountRepository.save(account);
            isUpdated = true;
        }
        return isUpdated;
    }


}

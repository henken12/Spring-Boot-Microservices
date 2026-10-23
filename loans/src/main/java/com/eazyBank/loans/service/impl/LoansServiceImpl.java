package com.eazyBank.loans.service.impl;

import com.eazyBank.loans.constants.LoansConstants;
import com.eazyBank.loans.dto.LoansDto;
import com.eazyBank.loans.entity.Loans;
import com.eazyBank.loans.exception.LoanAlreadyExistsException;
import com.eazyBank.loans.exception.ResourceNotFoundException;
import com.eazyBank.loans.mapper.LoanMapper;
import com.eazyBank.loans.repository.LoansRepository;
import com.eazyBank.loans.service.ILoanService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Random;
@Service
@AllArgsConstructor
public class LoansServiceImpl implements ILoanService {

    private LoansRepository loansRepository;


    /**
     * @param mobileNumber - Mobile Number of the Customer
     */
    @Override
    public void createLoan(String mobileNumber) {
    Optional<Loans> optionalLoans = loansRepository.findByMobileNumber(mobileNumber);
    if(optionalLoans.isPresent()){
       throw new LoanAlreadyExistsException("loan already registered with given mobileNumber :" +mobileNumber);
    }
    loansRepository.save(createNewLoan(mobileNumber));
    }

    private Loans createNewLoan(String mobileNumber){
        Loans newLoan = new Loans();
        long randomLoanNumber = 100000000000L+ new Random().nextInt(900000000);
        newLoan.setLoanNumber(Long.toString(randomLoanNumber));
        newLoan.setMobileNumber(mobileNumber);
        newLoan.setLoanType(LoansConstants.HOME_LOAN);
        newLoan.setTotalLoan(LoansConstants.NEW_LOAN_LIMIT);
        newLoan.setAmountPaid(0);
        newLoan.setOutstandingAmount(LoansConstants.NEW_LOAN_LIMIT);
        return newLoan;
    }
    /**
     *
     * @param mobileNumber - Input mobile Number
     * @return Loan Details based on a given mobileNumber
     */
    @Override
    public LoansDto fetchLoan(String mobileNumber) {
        Loans loans = loansRepository.findByMobileNumber(mobileNumber).orElseThrow(
                ()->new ResourceNotFoundException("Loan","mobileNumber",mobileNumber)
        );
       return LoanMapper.mapToLoansDTo(loans,new LoansDto());

    }
    /**
     *
     * @param loansDto - LoansDto Object
     * @return boolean indicating if the update of loan details is successful or not
     */
    @Override
    public boolean updateLoan(LoansDto loansDto) {
        Loans loans = loansRepository.findByLoanNumber(loansDto.getLoanNumber()).orElseThrow(
                ()->new ResourceNotFoundException("Loans","loanNumber", loansDto.getLoanNumber())
        );
        LoanMapper.mapToLoans(loansDto,loans);
        loansRepository.save(loans);
        return true;
    }
    /**
     * @param mobileNumber - Input MobileNumber
     * @return boolean indicating if the delete of loan details is successful or not
     */
    @Override
    public boolean deleteLoan(String mobileNumber) {
        Loans loans = loansRepository.findByMobileNumber(mobileNumber).orElseThrow(
                ()->new ResourceNotFoundException("Loans","mobileNumber",mobileNumber)
        );
        loansRepository.deleteById(loans.getLoanId());
        return true;
    }
}

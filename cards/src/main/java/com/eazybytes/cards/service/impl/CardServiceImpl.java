package com.eazybytes.cards.service.impl;

import com.eazybytes.cards.constants.CardsConstants;
import com.eazybytes.cards.dto.CardsDto;
import com.eazybytes.cards.entity.Cards;
import com.eazybytes.cards.exeption.CardsAlreadyExistsException;
import com.eazybytes.cards.exeption.ResourceNotFoundException;
import com.eazybytes.cards.mappers.CardsMapper;
import com.eazybytes.cards.repository.CardsRepository;
import com.eazybytes.cards.service.ICardsService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Random;

@Service
@AllArgsConstructor
public class CardServiceImpl implements ICardsService {
    @Autowired
    private CardsRepository cardsRepository;

    /**
     *
     * @param mobileNumber - Mobile Number of the Customer
     */
    @Override
    public void createCard(String mobileNumber) {
        Optional<Cards> optionalCards = cardsRepository.findByMobileNumber(mobileNumber);
        if(optionalCards.isPresent()){
            throw new CardsAlreadyExistsException("Card already registered with given mobileNumber :"+mobileNumber);
        }
        cardsRepository.save(createNewCard(mobileNumber));
    }

    /**
     *
     * @param mobileNumber mobile number of the customer
     * @return the new card detail
     */
    private Cards createNewCard(String mobileNumber){
        Cards newCard = new Cards();
        long randomCardNumber = 100000000000L + new Random().nextInt(900000000);
        newCard.setCardNumber(Long.toString(randomCardNumber));
        newCard.setMobileNumber(mobileNumber);
        newCard.setCardType(CardsConstants.CREDIT_CARD);
        newCard.setTotalLimit(CardsConstants.NEW_CARD_LIMIT);
        newCard.setAmountUsed(0);
        newCard.setAvailableAmount(CardsConstants.NEW_CARD_LIMIT);

        return newCard;
    }

    /**
     *
     * @param mobileNumber - Input mobile Number
     * @return Card Details based on a given mobileNumber
     */

    @Override
    public CardsDto fetchCard(String mobileNumber) {
       Cards cards = cardsRepository.findByMobileNumber(mobileNumber).orElseThrow(
               ()->new ResourceNotFoundException("Card","mobileNumber",mobileNumber)
       );
       return CardsMapper.maptoCardsDto(cards,new CardsDto());
    }

    /**
     *
     * @param cardsDto - CardsDto Object
     * @return boolean indicating if the update of card details is successful or not
     */
    @Override
    public boolean updateCard(CardsDto cardsDto) {
        Cards cards = cardsRepository.findByCardNumber(cardsDto.getCardNumber()).orElseThrow(
                ()->new ResourceNotFoundException("Card","cardNumber",cardsDto.getCardNumber())
        );
        CardsMapper.mapToCards(cardsDto,cards);
        cardsRepository.save(cards);
        return true;
    }
    /**
     * @param mobileNumber - Input MobileNumber
     * @return boolean indicating if the delete of card details is successful or not
     */
    @Override
    public boolean deleteCards(String mobileNumber) {
      Cards cards = cardsRepository.findByMobileNumber(mobileNumber).orElseThrow(
               ()->new ResourceNotFoundException("Card","mobileNumber",mobileNumber)
       );
       cardsRepository.deleteById(cards.getCardId());
       return true;
    }
}

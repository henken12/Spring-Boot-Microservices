package com.eazybytes.cards.exeption;

public class ResourceNotFoundException extends RuntimeException{

    public ResourceNotFoundException(String resourceName, String fieldName, String fieldValue){
        super(String.format("%s not found with given inpute data %s : '%s'", resourceName,fieldName, fieldValue));
    }
}

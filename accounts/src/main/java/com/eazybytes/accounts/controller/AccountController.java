package com.eazybytes.accounts.controller;

import com.eazybytes.accounts.constants.AccountsConstants;
import com.eazybytes.accounts.dto.AccountsContactInfoDto;
import com.eazybytes.accounts.dto.CustomerDto;
import com.eazybytes.accounts.dto.ErrorResponseDto;
import com.eazybytes.accounts.dto.ResponseDto;
import com.eazybytes.accounts.service.IAccountsService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;



//this annotation show information in the swagger ui
@Tag(
        name = "CRUD REST APIs for Accounts in EazyBank",
        description = "CRUD REST APIs in EasyBank to CREATE,UPDATE,FETCH AND DELETE account details"
)
@RestController
@RequestMapping(value = "/api",produces = {MediaType.APPLICATION_JSON_VALUE})
@Validated
public class AccountController {

    private static final Logger logger = LoggerFactory.getLogger(AccountController.class);

    private final IAccountsService iAccountsService;

    public AccountController(IAccountsService iAccountsService) {
        this.iAccountsService = iAccountsService;
    }

    @Value("${build.version}")
    private String buildVersion;

    @Autowired
    private Environment environment;

    @Autowired
    private AccountsContactInfoDto accountsContactInfoDto;

// this shows information with in the post method in swagger ui
    @Operation(
            summary = "Creat Account REST API",
            description = "REST API to creat new Customer & Account inside EazyBank"
    )
    @ApiResponse(
            responseCode = "201",
            description = "HTTP status CREATED"
    )
@PostMapping("/create")
  public ResponseEntity<ResponseDto> createAccount(@Valid @RequestBody CustomerDto customerDto){

      iAccountsService.createAccount(customerDto);
      return ResponseEntity
              .status(HttpStatus.CREATED)
              .body(new ResponseDto(AccountsConstants.STATUS_201,AccountsConstants.MESSAGE_201));
  }

  @Operation(
          summary = "Fetch Account Details REST API",
          description = "REST API to fetch Customer & Account details based on a mobile number"
  )
 @ApiResponses({
         @ApiResponse(
                 responseCode = "200",
                 description = "Http status ok"),
         @ApiResponse(
                 responseCode ="500",
         description = "HTTP status Internal Server Error",
         content = @Content(
                    schema = @Schema(implementation = ErrorResponseDto.class)
         )
                    )

 })

  @GetMapping("/fetch")
  public ResponseEntity<CustomerDto> fetchAccountDetails(@RequestParam
                                                           @Pattern(regexp = "(^$|[0-9]{10})",message = "Mobile number must be 10 digits")
                                                           String mobileNumber){
            CustomerDto customerDto = iAccountsService.fetchAccount(mobileNumber);
            return ResponseEntity.status(HttpStatus.OK).body(customerDto);
  }
  @Operation(
          summary = "update Account Details REST API",
          description = "REST API to update Customer & Account details based on a account number"
  )
  @ApiResponses({
          @ApiResponse(
                  responseCode = "200",
                  description = "HTTP Status OK"
          ),
          @ApiResponse(
                  responseCode = "417",
                  description = "Expectation failed"
          ),
          @ApiResponse(
                  responseCode = "500",
                  description = "HTTP Status Internal Server Error",
                  //since out open api documentation can't scan global exeption
                  //we need to provide to provide in shema here
                  content = @Content(
                          schema = @Schema(implementation = ErrorResponseDto.class)
                  )
          )
  })

  @PutMapping("/update")
    public ResponseEntity<ResponseDto> updateAccountDetails(@Valid @RequestBody CustomerDto customerDto){
    boolean isUpdated = iAccountsService.updateAccount(customerDto);
      if (isUpdated) {
          return ResponseEntity.status(HttpStatus.OK)
                  .body(new ResponseDto(AccountsConstants.STATUS_200,AccountsConstants.MESSAGE_200));
      }else{
          return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED)
                  .body(new ResponseDto(AccountsConstants.STATUS_417,AccountsConstants.MESSAGE_417_UPDATE));
      }
  }

  @Operation(
          summary = "Delete Account & Customer Details REST API",
          description = "REST API to delete Customer & Account details based on a mobile number"
  )
  @ApiResponses({
          @ApiResponse(
                  responseCode = "200",
                  description = "HTTP Status OK"
          ),
          @ApiResponse(
                  responseCode = "417",
                  description = "Expectation failed"
          ),
          @ApiResponse(
                  responseCode = "500",
                  description = "HTTP Status Internal Server Error"
          )
  })

  @DeleteMapping("/delete")
    public ResponseEntity<ResponseDto> deleteAccountDetails(@RequestParam
                                                              @Pattern(regexp = "(^$|[0-9]{10})",message = "Mobile number must be 10 digits")
                                                              String mobileNumber){
    boolean isDeleted = iAccountsService.deleteAccount(mobileNumber);
    if(isDeleted){
        return ResponseEntity.status(HttpStatus.OK)
                .body(new ResponseDto(AccountsConstants.STATUS_200,AccountsConstants.MESSAGE_200));
    }else{
        return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED)
                .body(new ResponseDto(AccountsConstants.STATUS_417,AccountsConstants.MESSAGE_417_DELETE));
    }
  }
    @Operation(
            summary = "Get build information ",
            description = "Get Build information that is deployed into accounts microservice"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Http status ok"),
            @ApiResponse(
                    responseCode ="500",
                    description = "HTTP status Internal Server Error",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponseDto.class)
                    )
            )

    })
  @GetMapping("/build-info")
  public ResponseEntity<String> getBuildInfo(){
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(buildVersion);
  }
    @Operation(
            summary = "Get Java version ",
            description = "Get Java version details that is installed into accounts microservice"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Http status ok"),
            @ApiResponse(
                    responseCode ="500",
                    description = "HTTP status Internal Server Error",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponseDto.class)
                    )
            )

    })
    @RateLimiter(name="getJavaVersion",fallbackMethod = "getJavaVersionFallback")
    @GetMapping("/java-version")
    public ResponseEntity<String> getJavaVersion(){
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(environment.getProperty("JAVA_HOME"));
    }
    public ResponseEntity<String> getJavaVersionFallback(){
        return ResponseEntity
                .status(HttpStatus.OK)
                .body("Java 24");
    }

    @Operation(
            summary = "Get Contact Info ",
            description = "contact info detail that can be reached out in case of any issues"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Http status ok"),
            @ApiResponse(
                    responseCode ="500",
                    description = "HTTP status Internal Server Error",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponseDto.class)
                    )
            )

    })
    @Retry(name="getContactInfo",fallbackMethod = "getContactInfoFallback")
    @GetMapping("/contact-info")
    public ResponseEntity<AccountsContactInfoDto> getContactInfo(){
        logger.debug("getContactInfo() method Invoke");
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(accountsContactInfoDto);
    }

    public ResponseEntity<String> getContactInfoFallback(Throwable throwable){
        logger.debug("getContactInfoFallback() method Invoked");
        return ResponseEntity
                .status(HttpStatus.OK)
                .body("0.9");
    }
}

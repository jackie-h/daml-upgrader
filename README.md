#Upgrades

##Upgrade Situations

- Contract logic changes
  - new methods
  - no method changes but logic within a method changes
    - add validation
    - change choices
- Contract inputs change
  - adding an input
  - removing an input
  - changing type of an input
  - changing name of an input
- Upgrades of standard libraries 
- Upgrades of data object dependencies (CDM)

##What changes the contract hash?

##Controls
Once released and in use we need to be careful to
- Keep an immutable record of that contract
- Must be careful about what we deploy, removing DAR file or redeploying can 

##Options

###A) Keep all live versions in trunk

- /MyContractV1
  - /MyContractV1
- /MyContractV2
  - /MyContractV2

Challenges:
 - Should not redeploy DAR that is already in use. i.e. if MyContractV1 has not changed, no need to redeploy



##Automated Upgrades



    

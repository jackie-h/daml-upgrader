#DAML Smart Contract Upgrades

Smart contracts are legally binding (or can be) so changes need to be approved by all parties
Smart contracts package data with logic, data schemas need migration


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
    - what if just giving type an alias/name
  - changing name of an input
- Contract name change
- Moving contract to another module but name same
- Upgrades of standard libraries 
- Upgrades of data object dependencies (CDM)

Difference between changes that would change the database schema stored, vs ones that just alter the logic.

- Should we do a "base contract" for the data and handle all logic as extensions ?
- How can interfaces help?
- Do all contracts need upgrades ? Some don't persist state ? If none are in flight, can remove ?
- Does the operator have the ability to know all contracts in use ?

- Generated java and javascript code - that is per version, is it possible to generate across versions
  - New fields - handled as optional ?
  - Interfaces ?
  - How do extension contracts help, if at all ?
- Could we take a view that you are not allowed to delete fields ? No breaking changes?
- Difference between getter API's to view data and contract actions that allow people to do things
- Are there any performance challenges with using extension approach ?
- What happens if there is a change to something fundamental such as Party ?
- Snapshot builds vs release builds ? Do we want to use that or just git hash ? Can we do auto-packaging/versioning ?
- Versioning of DAR files, how do we enforce major/minor versions ?

##Identity

###How does a live contract instance identify which code was used to create it?

DAML files are compiled into DALF archives. The DALF archive hash is the identifier for the package.

A template "Foo" in module "Bar" will be identified as (Foo,Bar,<DALF hash>) 

> Any code – i.e., Daml templates – to be uploaded must compiled down to the Daml-LF language. The unit of packaging for Daml-LF is the .dalf file. Each .dalf file is uniquely identified by its package identifier, which is the hash of its contents. Templates in a .dalf file can reference templates from other .dalf files, i.e., .dalf files can depend on other .dalf files. A .dar file is a simple archive containing multiple .dalf files, and has no identifier of its own. The archive provides a convenient way to package .dalf files together with their dependencies. The Ledger API supports only .dar file uploads. Internally, the ledger implementation need not (and often will not) store the uploaded .dar files, but only the contained .dalf files.

[DAML Docs:Package Identifiers](https://docs.daml.com/concepts/identity-and-package-management.html#package-formats-and-identifiers)


> All types, including templates and records are identified by the triple (entity name, module name, package hash).

> The package hash is the primary and only identifier for a package that’s guaranteed to be available and preserved.

[DAML Docs:Dependencies Hashes and Identifiers](https://docs.daml.com/daml/intro/9_Dependencies.html#hashes-and-identifiers)

##Versioning

DAML follows semantic versioning

> All Daml components follow Semantic Versioning. In short, this means that there is a well defined “public API”, changes or breakages to which are indicated by the version number.
>
> Stable releases have versions MAJOR.MINOR.PATCH. Segments of the version are incremented according to the following rules:
>
> MAJOR version when there are incompatible API changes,
> 
> MINOR version when functionality is added in a backwards compatible manner, and
> 
> PATCH version when there are only backwards compatible bug fixes.

[DAML Docs:Versioning](https://docs.daml.com/support/releases.html#versioning)

##Controls
Once released and in use we need to be careful to
- Keep an immutable record of that contract
- Must be careful about what we deploy, removing DAR file or redeploying can break prod

##Scenarios

- New development, contract is not in production yet, all changes allowed, no need to do a new version
  - Potentially still want to use versions upgrades if downstream dev dependencies (API/UI/BLIP) ?

- Contract is already in production and used
  - Must not be allowed to change it without creating a new version and upgrade
    - Are there any situations where it can be changed and won't change hash?

##Requirements
- Ability to stop creation of contracts for obsolete templates we are trying to decommission
- Emergency upgrades - need a "breakglass" force upgrade for bad bugs ? Or ability to freeze actions on contracts until upgraded?
- Timelines - do we force upgrades to be accepted within a certain timeline ? Need to control how many versions are out
- Developer should review upgrade contracts, tooling can generate them but should be reviewed.

##Solutions

###Code base structure

DAML recommendations: 

> separate concerns which are likely to change at different rates into separate packages

> separate tests from main templates 

[DAML Docs:Structuring Projects](https://docs.daml.com/daml/intro/9_Dependencies.html#structuring-projects)

Want small self-contained DAR files.

In a GS code base.
Options:
- Same name e.g. Gs.CarbonContract
- Same name but with version, e.g. Gs.CarbonContractV1 and Gs.CarbonContractV2

###A) Keep all live versions in trunk

- /MyContractV1
  - /MyContractV1
- /MyContractV2
  - /MyContractV2

Challenges:
 - Should not redeploy DAR that is already in use. i.e. if MyContractV1 has not changed, no need to redeploy
   - Can overcome by doing a check on the hash of prod

###B) Separate tiny projects for each contract
Benefits
- Small self-contained projects
- Can have faster build times (depends on build tech, parallelism can help)

Downsides
- When you have dependencies, have to release dependent projects first, difficult to test whole thing
- Upgrades of SDK/stand libs have to be done in each project

###C) Mono-repo style micro-services
- Do a diff and only redeploy if changed
https://serverless-stack.com/chapters/deploying-only-updated-services.html

Benefits 
- All code is there, can refactor across in one go
- Changes across multiple packages can easily be made together

##What constitutes a new version of API
- Adding a new field - non-breaking ?

##What needs to happen to upgrade

###Normal release
1. Starting state: my-contract-1.0.dar is in prod in use.
2. Create and release new version: my-contract-2.0.dar
3. API & UI need to continue to support both ?
4. Create and release upgrade contracts: my-contract-upgrade-v1-v2-1.0.jar
5. Communicate new release to parties
6. Parties accept upgrade contracts, different parties may accept at different times
7. Verify all parties have accepted and no contracts remain on chain with v1.0.dar
8. Update API/UI so it no longer queries for my-contract-1.0.dar items
9. Remove my-contract-1.0.dar from Canton nodes

###Emergency

##Automated Upgrade Tooling

This is what it would do

Given two git hashes or two DAR files, for contracts of the same name 
  - Determine if have changes that break DALF hash
    - no? great - no action
    - yes? create an upgrade contract

Upgrade contract
 - Identify which parties need to agree
 - Create proposal and agreement
 - Inputs change?
   - No? 
   - Yes? Need to figure out if those can be defaulted

    

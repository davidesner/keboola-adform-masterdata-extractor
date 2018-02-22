# AdForm masterdata extractor for KBC
Extractor for Keboola Connection allowing automated downloads from AdForm masterdata service.

**For this application to work, you need:**

 - Agency account with permission to access Adform External API (https://api.adform.com/)
 - Master Data service enabled 
  
Once the contract is signed Adform will enable Master Data service and send you the credentials. Please note, that accessing External API places additional requirements on your account security and password complexity.

Please contact Adform support, if you have any questions.

**NOTE:** You can also use WEB UI (https://www.adform.com/masterdata?setup={setupId}), where ```{setupId}``` is your unique Master Data setup identificator, to preview and download files using WEB browser.

## Funcionality
AdForm masterdata service provides data dumps usually at hourly intervals. This application allows you to retrieve tables of your choice within the specified interval.

User may specify datasets to be extracted. There are two types of data tables provided by masterdata. 

The first one provides data usually at hourly intervals and in a period of maximum one week. These tables are then imported incrementally to the specified tables in KBC. Prefixes are defined using the **Datasets** config parameter described below.

**IMPORTANT NOTE:** Configuration does not allow to specify the primary keys of imported tables to prevent the mistyping of the primary key column names. Instead, user must set the primary keys manually in the KBC UI within the *STORAGE* section after the first successfull import. Otherwise, the incremental import will not work correctly! See the screenshot below..

![](https://github.com/davidesner/keboola-adform-masterdata-extractor/blob/master/screens/PK_Key_setting.png)

Metadata section contains metadata i.e. campaign names for the fact tables. It changes less often and all historical data is contained in the newly imported table. Therefore, the import of metadata tables is NOT incremental and overwrites the original table in Storage. The user hence does not need to specify primary keys of metadata tables manually to ensure correct import.



## Configuration
AdForm masterdata service provides data dumps usually at hourly intervals. This application allows you to retrieve tables of your choice within the specified interval.

Configuration takes folowing parameters:
*

 - **User** - (REQUIRED) user name (login) of your AdForm MasterData service account
 -  **Password** - (REQUIRED) password of your AdForm MasterData service account
 - **Setup ID** - (REQUIRED) Your unique Master Data setup identificator.
 - **Date To** - (OPTIONAL) time until which you want to retrieve the data, i.e. the upper boundary of the time interval you want to retrieve - data before this time (excluded) will be retrieved. 
The time has to be specified in the following format: *05-10-2015 21:00*. If not specified data until *NOW* will be retrieved.
 - **Days interval** - (REQUIRED) time interval within which you want to retrieve the data. Specified in days. For example if the value is 5 and `Date To` is not specified, data dated between today and five days ago will be retrieved.
 - **Output bucket** - (REQUIRED) name of the bucket in KBC. e.g. *in.c-main*
 - **Datasets** - (REQUIRED) list of datasets you want to retrieve. e.g. *[Impression, Click, Trackingpoint]* 
 - **Metadata** - (OPTIONAL) the list of metadata tables (prefixes) you want to retrieve. e.g. *[geolocations, campaigns]*
 - **File charset** - Specify file encoding of the returned dataset. By default UTF-8, however it may vary in some cases. If specified incorrectly, the import to Storage will fail.

### Sample configuration
Lets say we want to retrieve data from tables with prefixes *Impression, Click* and *Trackingpoint* within the interval *[08-10-2015 0:00, 12-10-2015 0:00)*.
We also want to retrieve the metadata tables with prefixes *geolocations* and *campaigns* and we want the results in the bucket *in.c-main*

The configuration would look like this:
![](https://github.com/davidesner/keboola-adform-masterdata-extractor/blob/master/screens/config.png)




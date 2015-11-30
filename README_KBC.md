# AdForm masterdata extractor for KBC
Extractor for Keboola Connection allowing automated downloads from AdForm masterdata service.

## Funcionality
AdForm masterdata service provides data dumps usually at hourly intervals. This application allows you to retrieve tables of your choice within the specified interval.

User may specify prefixes of the tables to be extracted. There are two types of data tables provided by masterdata. 

The first one provides data usually at hourly intervals and in a period of maximum one week. These tables are then imported incrementally to the specified tables in KBC. Prefixes are defined using the **prefixes** config parameter described below.

**IMPORTANT NOTE:**Configuration does not allow to specify the primary keys of imported tables to prevent the mistyping of the primary key column names. Instead, user must set the primary keys manually in the KBC UI within the *STORAGE* section after the first successfull import. See the screenshot below..

Metadata section is another kind of data provided by masterdata. It contains metadata i.e. campaign names for the fact tables. It changes less often and all historical data is contained in the newly imported table. Therefore, the import of metadata tables is NOT incremental and overwrites the original table in Storage. The user hence does not need to specify primary keys of metadata tables manually to ensure correct import.

![](https://github.com/davidesner/keboola-adform-masterdata-extractor/blob/master/screens/PK_Key_setting.png)

## Configuration
AdForm masterdata service provides data dumps usually at hourly intervals. This application allows you to retrieve tables of your choice within the specified interval.

Configuration takes folowing parameters:
* **user** - (REQUIRED) user name (login) of your AdForm MasterData service account
* **#pass** - (REQUIRED) password of your AdForm MasterData service account
* **mdListUrl** - (REQUIRED) your MasterData list url. i.e. *http://masterdata.adform.com:8652/list/XXX*, where the *XXX* is your MasterData ID.
* **dateTo** - (OPT) time until which you want to retrieve the data, i.e. the upper boundary of the time interval you want to retrieve - data before this time (excluded) will be retrieved. 
The time has to be specified in the following format: *05-10-2015 21:00*. If not specified data until *NOW* will be retrieved.
* **daysInterval** - (REQUIRED) time interval within which you want to retrieve the data. Specified in days. For example if the value is 5 and *dateTo* is not specified, data dated between today and five days ago will be retrieved.
* **bucket** - (REQUIRED) name of the bucket in KBC. e.g. *in.c-main*
* **prefixes** - (REQUIRED) list of prefixes of the tables you want to retrieve. e.g. *[Impression, Click, Trackingpoint]* NOTE: the values are case-sensitive! The result will be then uploaded to tables in KBC named as *bucket-name*.*prefix* e.g. *in.c-main.impression*
* **metaFiles** - (OPT) the list of metadata tables (prefixes) you want to retrieve. e.g. *[geolocations, campaigns]*

### Sample configuration
Lets say we want to retrieve data from tables with prefixes *Impression, Click* and *Trackingpoint* within the interval *[08-10-2015 0:00, 12-10-2015 0:00)*.
We also want to retrieve the metadata tables with prefixes *geolocations* and *campaigns* and we want the results in the bucket *in.c-main*

The configuration parameters would look like this:
```json
{
      "user": "AdForm-login",
      "#pass": "password",
      "mdListUrl" : "http://masterdata.adform.com:8652/list/XXXX",
      "daysInterval" : 4,
      "dateTo" : "12-10-2015 0:00",
      "bucket" : "in.c-main",
      "prefixes" : [ "Impression", "Click", "Trackingpoint" ],
      "metaFiles" : ["geolocations","campaigns"]
    }
```
![](https://github.com/davidesner/keboola-adform-masterdata-extractor/blob/master/screens/config.png)




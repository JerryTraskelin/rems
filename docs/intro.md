# Introduction

# API use the 1 minute version

Here we assume that the instance you want to talk to is the REMS demo instance at `rems2demo.csc.fi`.

You have an API-Key `42` that you must provide in the header `X-REMS-API-Key`.

Checking what catalogue items are available

```sh
curl -H "X-REMS-API-Key: 42" https://rems2demo.csc.fi/api/catalogue
```

Returns the JSON response with the catalogue items

```json
[
    {
        "formid": 1,
        "id": 1,
        "localizations": {
            "en": {
                "id": 1,
                "langcode": "en",
                "title": "ELFA Corpus, direct approval"
            },
            "fi": {
                "id": 1,
                "langcode": "fi",
                "title": "ELFA-korpus, suora hyv\u00e4ksynt\u00e4"
            }
        },
        "resid": "urn:nbn:fi:lb-201403262",
        "state": "enabled",
        "title": "non-localized title",
        "wfid": 1
    },

    ...
]
```

Some API endpoints also require `X-REMS-User-Id` header that is the REMS user id for the user that is represented. I.e. which user applies for a resource and which approves an application.

See other methods in the [Swagger API documentation](https://rems2demo.scs.fi/swagger-ui).

# UI deep linking

## Linking into catalogue

```
https://rems2demo.csc.fi/#/catalogue
```

## Linking into a new application

This application has items with catalogue item ids 2 and 3.

```
https://rems2demo.csc.fi/#/application?items=2,3
```

## Linking into an existing application

```
https://rems2demo.csc.fi/#/application/2
```

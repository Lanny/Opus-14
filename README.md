# Opus-14

A system for assessing the likely impact of social justice films.

## Style

- **2-space 4 lyfe!!!**
- Trust in [vim-clojure-static](https://github.com/guns/vim-clojure-static)
- Underscores and camel case are verboten in .clj files
- `use` and `:refer :all` are discouraged, only use them where using namespace
  prefixes would clearly hurt readability

## Environment

We hit a couple of external APIs, so you're going to need keys for them. We 
use [environ](https://github.com/weavejester/environ) for managing those. See
the link for details on how to set that up, the keys that need to be present in
the env map at runtime are:

- **tw-api-key**
- **tw-api-secret**
- **tw-access-token**
- **tw-access-secret**
- **klout-api-key**

## Development Notes

- We need training data in order to classify the cause/genre of films based on
  their plot descriptions. The project ships with a small data set in the 
  resources directory and a script `pull_lists.py` that can be used to scape
  these plot descriptions from IMDB and MAF.

## API Endpoints

### `/index`
**Description**: Data to be displayed on the home page of the app including
news and a set of recently added films.

**Method**: GET

**Parameters**: None

**Example response**:
```json
{
  "news": "See our rankings for this year's <a href=\"/blaa\">Sundance Nominees</a>",
  "films": [
    {
      "id": 42,
      "title": "Miss Representation",
      "score": 7.2,
      "posterUrl": "http://ia.media-imdb.com/images/M/MV5BNTM5X640_SY720_.jpg"
    },
    ...
  ]
}
```

### `/search`
**Descriprion**: List of films that match query criteria. One or both of 
`q` and `category` must be specified.

**Method**: GET

**Parameters**:

- `q`: A string compared against film titles
- `category`: A category to filter results by. Valid values can be found in
the `pull_list.py` file.
- `order`: One of `year` or `score` deciding how the returned values should be
sorted. Defaults to year.

**Example response**:
```json
{
  "q": "Representation",
  "cagegory": null,
  "order": "score",
  "films": [
    {
      "id": 42,
      "title": "Miss Representation",
      "score": 7.2,
      "category": "Women's Rights",
      "year": 2009,
      "posterUrl": "http://ia.media-imdb.com/images/M/MV5BNTM5X640_SY720_.jpg"
    },
    ...
  ]
}
```

### `/film`
**Descriprion**: Detailed info about a film and its score.

**Method**: GET

**Parameters**:

- `fid`: Opus internal ID of the film in question.

**Example response**:
```json
{
  "id": 42,
  "title": "Miss Representation",
  "category": "Women's Rights",
  "year": 2009,
  "posterUrl": "http://ia.media-imdb.com/images/M/MV5BNTM5X640_SY720_.jpg"
  "score": 7.2,
  "scoreComponents": {
    "directorExperience": 0,
    "firstTwitterResult": true,
    "firstFacebookResult: true,
    "kloutScore": 65,
    "intraLinkage": true,
    "extraLinkage": true,
    "hasTrailer: true,
    "runtime": 125,
    "ambiguousClassification": false
  }
}
```



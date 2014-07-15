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

## Development Notes

- Film and actor tables have a `fetched` field. This is to defer calling our
  data sources until we absoutely need to since most of them are really slow.
  If the `fetched` field is not set then you can not use the data, you have to
  pull it first.
- Internally we use `actor` to mean a human of any gender who works on a film
  in any capacity. This includes actresses, directors, and writers. This may
  or may not be represented in front-end code or in apis we pull from. What a
  given `actor` does on a project is indicated by their `role`. Roles are still
  gender neutral, i.e. both men and women appearing in a film are refered to as
  actors.

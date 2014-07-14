# Opus-14

A system for assessing the likely impact of social justice films.

## Style

- **2-space 4 lyfe!!!**
- Trust in [vim-clojure-static](https://github.com/guns/vim-clojure-static)
- Underscores and camel case are verboten in .clj files

## Development Notes

- Film and actor tables have a `fetched` field. This is to defer calling our
  data sources until we absoutely need to since most of them are really slow.
  If the `fetched` field is not set then you can not use the data, you have to
  pull it first.

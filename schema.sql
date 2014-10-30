CREATE TABLE films (
  id INTEGER PRIMARY KEY,

  title TEXT NOT NULL UNIQUE,      -- User editable
  twitter_username TEXT,           -- User editable
  facebook_url TEXT,               -- Sorta user editable
  website_url TEXT,                -- User editable
  director_name TEXT,              -- User editable

  description TEXT,                -- Non-editable
  category TEXT,                   -- From classifier

  idIMDB TEXT,                     -- From myapifilms
  plot TEXT,                       -- From myapifilms
  year INTEGER,                    -- From myapifilms
  urlPoster TEXT,                  -- From myapifilms

  -- Here be the score components --
  director_experience INTEGER,
  first_twitter_result INTEGER,
  first_facebook_result INTEGER,
  klout_score INTEGER,
  intalinkage INTEGER,             -- Do film assets link to eachother?
  extralinkage INTEGER,            -- Do partners link back to film?
  has_trailer INTEGER,
  runtime INTEGER,                 -- In minutes
  ambiguous_classification INTEGER,

  -- And ye ole' score itself --
  score REAL
)

-- Actors include directors
CREATE TABLE actors (
  id INTEGER PRIMARY KEY,
  idIMDB TEXT,
  fetched INTEGER DEFAULT 0,   -- Boolean, 0 if we haven't made the call 
                               -- to populate
  name TEXT,           -- From myapifilms
  urlPhoto TEXT        -- From myapifilms
);

CREATE TABLE credits (
  actors_id INTEGER,
  films_id INTEGER,
  role TEXT, -- What the actor did, e.x. director, actor, writer
  FOREIGN KEY(actors_id) REFERENCES actor(id) ON DELETE CASCADE,
  FOREIGN KEY(films_id) REFERENCES films(id) ON DELETE CASCADE,
  -- Can't be the director on one film twice
  UNIQUE (actors_id, films_id, role) ON CONFLICT REPLACE 
);

CREATE TABLE films (
  id INTEGER PRIMARY KEY,

  title TEXT NOT NULL UNIQUE,      -- User editable
  twitter_username TEXT,           -- User editable
  facebook_obj_id TEXT,            -- Sorta user editable
  website_url TEXT,                -- User editable
  director_name TEXT,              -- User editable

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
  ambigious_classification INTEGER,
  alist_talent INTEGER,

  -- And yo ole' score itself --
  score REAL
)

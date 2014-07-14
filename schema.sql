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
  role STRING, -- What the actor did, e.x. director, actor, writer
  FOREIGN KEY(actors_id) REFERENCES actor(id) ON DELETE CASCADE,
  FOREIGN KEY(films_id) REFERENCES films(id) ON DELETE CASCADE,
  UNIQUE (actors_id, films_id, role) -- Can't be the director on one film twice
);

CREATE TABLE films (
  id INTEGER PRIMARY KEY,
  idIMDB TEXT NOT NULL,
  fetched INTEGER DEFAULT 0
);

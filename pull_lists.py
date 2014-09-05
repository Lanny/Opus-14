#!/usr/bin/env python
import urllib
import json
import lxml.html

from lxml.cssselect import CSSSelector

keywords = [
    ('Human Rights', 'human-rights'),
    ('LBGT Rights', 'gay-interest'),
    ('Poverty', 'poverty'),
    ('Environmental Issues', 'environment'),
    ('War/Anit-War', 'anti-war'),
    ('Women\'s Rights', 'feminism'),
    ('Prison Reform', 'prison'),
    ('Healthcare', 'healthcare'),
    ('Immigration', 'immigration')
]

if __name__ == "__main__":
    export_data = {}

    for human_readable, keyword in keywords:
        print 'Processing keyword: %s' % human_readable

        base_url = 'http://www.imdb.com/search/keyword'
        min_data = 10
        selector = CSSSelector('div.lister-item-image img.loadlate')
        imdb_ids = []
        req_map = {
            'keywords': keyword,
            'sort': 'moviemeter,asc',
            'page': 1,
            'genres': 'Documentary'
        }

        while len(imdb_ids) < min_data:
            print '  Pulling page %d of IMDB results' % req_map['page']

            url = base_url + '?' + urllib.urlencode(req_map)
            res = urllib.urlopen(url)
            doc = lxml.html.parse(res)
            res.close()

            images = selector(doc)
            imdb_ids.extend([e.get('data-tconst') for e in images])

            req_map['page'] += 1

        export_data[human_readable] = []

        print '  Looking up plot descriptions for %d films' % len(imdb_ids)

        base_url = 'http://www.myapifilms.com/imdb'
        for imdb_id in imdb_ids:
            url = base_url + '?' + urllib.urlencode({'idIMDB': imdb_id})
            res = urllib.urlopen(url)

            data = json.load(res)

            print '    - Found plot for "%s"' % data['title']
            export_data[human_readable].append(data['plot'])

    
    fp = open('training_data.json', 'w')
    json.dump(export_data, fp)
    fp.close()

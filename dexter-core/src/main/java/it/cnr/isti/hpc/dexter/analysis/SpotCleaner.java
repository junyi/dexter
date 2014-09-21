/**
 *  Copyright 2014 Diego Ceccarelli
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package it.cnr.isti.hpc.dexter.analysis;

import it.cnr.isti.hpc.dexter.spot.cleanpipe.filter.Filter;
import it.cnr.isti.hpc.dexter.spot.cleanpipe.filter.ImageFilter;
import it.cnr.isti.hpc.dexter.spot.cleanpipe.filter.NumberFilter;
import it.cnr.isti.hpc.dexter.spot.cleanpipe.filter.SymbolFilter;
import it.cnr.isti.hpc.dexter.spot.cleanpipe.filter.TemplateFilter;
import it.cnr.isti.hpc.dexter.spot.cleanpipe.mapper.CityMapper;
import it.cnr.isti.hpc.dexter.spot.cleanpipe.mapper.Mapper;
import it.cnr.isti.hpc.dexter.spot.cleanpipe.mapper.QuotesMapper;
import it.cnr.isti.hpc.wikipedia.article.Article;
import it.cnr.isti.hpc.wikipedia.article.Link;

import java.io.IOException;
import java.io.StringReader;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

/**
 * @author Diego Ceccarelli <diego.ceccarelli@isti.cnr.it>
 * 
 *         Created on Sep 21, 2014
 */
public class SpotCleaner {
	StringBuilder sb;
	List<Mapper<String>> mappers;
	List<Filter<String>> filters;

	int maxSpotLength = 6;

	public SpotCleaner() {
		sb = new StringBuilder();
		mappers = new ArrayList<Mapper<String>>();
		mappers.add(new CityMapper());
		mappers.add(new QuotesMapper());

		filters = new ArrayList<Filter<String>>();
		filters.add(new NumberFilter());
		filters.add(new SymbolFilter());
		filters.add(new TemplateFilter());
		filters.add(new ImageFilter());
	}

	public String clean(String spot) throws IOException {
		try {
			spot = URLDecoder.decode(spot, "UTF-8");
		} catch (IllegalArgumentException e) {

		}

		SpotAnalyzer analyzer = new SpotAnalyzer();
		analyzer.lowercase(spot.length() > 4);

		TokenStream ts = analyzer
				.tokenStream("content", new StringReader(spot));

		CharTermAttribute termAtt = ts.addAttribute(CharTermAttribute.class);
		ts.reset();
		sb.setLength(0);
		int tokens = 0;
		while (ts.incrementToken()) {
			tokens++;
			sb.append(termAtt.toString());
			sb.append(' ');
			if (tokens > maxSpotLength) {
				analyzer.close();
				return "";
			}
		}
		if (sb.length() > 0)
			sb.setLength(sb.length() - 1);
		analyzer.close();
		// System.out.println(spot + " -> " + "[" + sb.toString() + "]");
		String finalSpot = sb.toString();
		for (Filter<String> filter : filters) {
			if (filter.isFilter(finalSpot)) {
				finalSpot = "";
			}
		}
		return finalSpot;
	}

	public Set<String> enrich(String spot) throws IOException {
		Set<String> hashSet = new HashSet<String>();
		hashSet.add(spot);
		for (Mapper<String> mapper : mappers) {
			for (String s : mapper.map(spot)) {
				String t = clean(s);
				if (!t.isEmpty()) {
					hashSet.add(clean(t));
				}
			}

		}
		return hashSet;
	}

	public Set<String> getAllSpots(Article a) throws IOException {
		Set<String> spots = new HashSet<String>();
		spots.addAll(enrich(a.getTitle()));
		if (a.isRedirect()) {
			spots.addAll(enrich(a.getRedirectNoAnchor()));
		} else {

			for (Link l : a.getLinks()) {
				spots.addAll(enrich(l.getDescription()));
			}
		}
		return spots;
	}

}

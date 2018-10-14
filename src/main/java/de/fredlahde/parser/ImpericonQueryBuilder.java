package de.fredlahde.parser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ImpericonQueryBuilder {
    public static void main(String[] args) throws IOException {
        var inFile = new File("impericon.html");
        BufferedReader buff = new BufferedReader(new FileReader(inFile));

        String line;
        StringBuilder content = new StringBuilder();
        try (buff) {
            line = buff.readLine();
            while (line != null) {
                content.append(line);
                line = buff.readLine();
            }
        }

        var doc = Jsoup.parse(content.toString());
        var bands = doc.getElementsByTag("a").stream()
                .map(LinkHolder::new)
                .peek(LinkHolder::parseBands)
                .filter(entry -> entry.name != null)
                .collect(Collectors.toList());

        var tops = doc.getElementsByTag("a").stream()
                .map(LinkHolder::new)
                .peek(LinkHolder::parseTops)
                .filter(entry -> entry.name != null)
                .collect(Collectors.toList());

        filterDuplicates(bands);
        filterDuplicates(tops);
        var bandRegex = args[0];
        var topRegex = args[1];

        var totalQuery = "";
        if (null != bandRegex) {
            var pattern = Pattern.compile(bandRegex);

            var matchedBands = bands.stream()
                    .filter(b -> b.match(pattern))
                    .collect(Collectors.toList());
            if (!matchedBands.isEmpty()) {
                totalQuery = totalQuery + buildQuery("band", matchedBands);
            }
        }

        if (null != topRegex) {
            var pattern = Pattern.compile(topRegex);

            var matchedTos = tops.stream()
                    .filter(b -> b.match(pattern))
                    .collect(Collectors.toList());

            if (!matchedTos.isEmpty()) {
                if (totalQuery.isEmpty()) {
                    totalQuery = totalQuery + buildQuery("type_top", matchedTos);
                } else {
                    totalQuery = totalQuery + buildQuery("type_top", matchedTos).replaceFirst("\\?", "&");
                }
            }
            System.out.println(totalQuery);
        }
    }

    private static String buildQuery(String cat, List<LinkHolder> entries) {
        if (entries.isEmpty())
            return "";

        var query = new StringBuilder();

        query.append(String.format("?%s=%s", cat, entries.get(0).bandID));

        if (entries.size() > 1) {
            for (int i = 1; i < entries.size(); i++) {
                query.append(String.format(",%s", entries.get(i).bandID));
            }
        }

        return query.toString();
    }

    private static void filterDuplicates(List<LinkHolder> input) {
        var seen = new HashMap<String, LinkHolder>();

        for (LinkHolder linkHolder : input) {
            if (!seen.containsKey(linkHolder.bandID)) {
                seen.put(linkHolder.bandID, linkHolder);
            }
        }

        input.clear();
        input.addAll(seen.values());
    }
}

class LinkHolder {
    private static final Pattern bandLinkPattern = Pattern.compile("\\?band=(\\d+)");
    private static final Pattern topLinkPattern = Pattern.compile("\\?type_top=(\\d+)");

    String name, bandID;
    private Element elem;

    LinkHolder(Element elem) {
        this.elem = elem;
    }

    void parseBands() {
        var matcher = bandLinkPattern.matcher(elem.attr("href"));
        if (!matcher.find()) {
            return;
        }

        this.name = elem.attr("title").toLowerCase();
        this.bandID = matcher.group(1);
    }

    void parseTops() {
        var matcher = topLinkPattern.matcher(elem.attr("href"));
        if (!matcher.find()) {
            return;
        }

        this.name = elem.attr("title").toLowerCase();
        this.bandID = matcher.group(1);
    }

    boolean match(Pattern pattern) {
        return (pattern.matcher(name)).find();
    }
}

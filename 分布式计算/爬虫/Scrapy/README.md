# Scrapy CheatSheet

# 递归爬取

```py
class DmozSpider(CrawlSpider):
    name = "dmoz"
    allowed_domains = ["mydomain.nl"]
    start_urls = [
        "http://www.mydomain.nl/Zuid-Holland"
    ]

    rules = (Rule(SgmlLinkExtractor(allow=('*Zuid-Holland*', )), callback='parse_winkel', follow=True),)

    def parse_winkel(self, response):
        sel = Selector(response)
        sites = sel.xpath('//ul[@id="itemsList"]/li')
        items = []

        for site in sites:
            item = WinkelItem()
            item['adres'] = site.xpath('.//a/text()').extract(), site.xpath('text()').extract(), sel.xpath('//h1/text()').re(r'winkel\s*(.*)')
            items.append(item)
        return items
```

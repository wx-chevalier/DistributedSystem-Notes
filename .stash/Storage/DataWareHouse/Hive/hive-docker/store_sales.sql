drop table store_sales;
drop table store_sales_orc;

create table if not exists store_sales
(
    ss_sold_date_sk           int,
    ss_sold_time_sk           int,
    ss_item_sk                int,
    ss_customer_sk            int,
    ss_cdemo_sk               int,
    ss_hdemo_sk               int,
    ss_addr_sk                int,
    ss_store_sk               int,
    ss_promo_sk               int,
    ss_ticket_number          int,
    ss_quantity               int,
    ss_wholesale_cost         float,
    ss_list_price             float,
    ss_sales_price            float,
    ss_ext_discount_amt       float,
    ss_ext_sales_price        float,
    ss_ext_wholesale_cost     float,
    ss_ext_list_price         float,
    ss_ext_tax                float,
    ss_coupon_amt             float,
    ss_net_paid               float,
    ss_net_paid_inc_tax       float,
    ss_net_profit             float
)
row format delimited fields terminated by '|';

load data local inpath '/opt/files/store_sales.txt' overwrite into table store_sales;

create table if not exists store_sales_orc like store_sales;
alter table store_sales_orc set fileformat orc;

insert overwrite table store_sales_orc select * from store_sales;

select ss_sold_date_sk, count(ss_quantity) as quantity_sold from store_sales_orc group by ss_sold_date_sk order by quantity_sold desc limit 10;

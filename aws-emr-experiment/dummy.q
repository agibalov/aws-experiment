drop table if exists InputTable;
drop table if exists OutputTable;

create external table InputTable(id string, text string)
row format delimited fields terminated by ','
location '${INPUT}';

create external table OutputTable(text string, count int)
row format delimited fields terminated by ','
location '${OUTPUT}';

insert into OutputTable(text, count)
select text, count(*)
from InputTable
group by text;

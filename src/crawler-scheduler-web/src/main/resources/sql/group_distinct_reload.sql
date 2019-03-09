--默认旧代码的版本为0，以后新建任务默认起始版本为1
alter table crawler_job add versionNo int default 0;

--版本表
create table sw_version
(
   id bigint(20) not null AUTO_INCREMENT,
   versionNo int,
   jobId int,
   title VARCHAR(50),
   descri VARCHAR(5000),
   creator VARCHAR(255),
   modifier VARCHAR(255),
   gmtCreate datetime,
   gmtModified datetime,
   `delete` int,
   primary key (id)
);
--分组表
create table sw_group
(
   id bigint(20) not null AUTO_INCREMENT,
   name VARCHAR(255),
   otsName VARCHAR(255),
   mqTag VARCHAR(255),
   creator VARCHAR(255),
   modifier VARCHAR(255),
   gmtCreate datetime,
   gmtModified datetime,
   `delete` int,
   primary key (id)
);
--init参数
insert into sw_group values(1,'签约媒体','xhzy_xinhua_news_v2','xhs_partners','system','system','2008-01-01 00:00:00','2008-01-01 00:00:00',0);
insert into sw_group values(2,'热门新闻','xhzy_hot_news_v2','hot_news','system','system','2008-01-01 00:00:01','2008-01-01 00:00:01',0);
insert into sw_group values(3,'英文新闻','xhzy_en_news','news_en','system','system','2008-01-01 00:00:02','2008-01-01 00:00:02',0);
insert into sw_group values(4,'精选新闻','xhzy_news_v2','xhs_partners','system','system','2008-01-01 00:00:03','2008-01-01 00:00:03',0);
insert into sw_group values(5,'通知栏新闻','xhzy_phone_message_news','hot_news','system','system','2008-01-04 00:00:04','2008-01-01 00:00:04',0);
insert into sw_group values(6,'基础数据存储','xhzy_basedata','','system','system','2008-01-01 00:00:05','2008-01-01 00:00:05',0);
insert into sw_group values(7,'学习强国数据存储','xhzy_xuexi_data','','system','system','2008-01-01 00:00:06','2008-01-01 00:00:06',0);
insert into sw_group values(9,'ots不存储','','','system','system','2008-01-01 00:00:08','2008-01-01 00:00:08',0);
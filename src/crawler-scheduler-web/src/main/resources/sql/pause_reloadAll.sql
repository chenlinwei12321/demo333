alter table crawler_job add isPause int default 0;
alter table crawler_job add isFull int default 0;
#配置华增加xpath范围
alter table sw_module_edit add part VARCHAR(2555) default "[]";
alter table sw_module_pub add part VARCHAR(2555) default "[]";
#爬虫模板
create table sw_module_template
(
   id bigint(20) not null AUTO_INCREMENT,
   templateName VARCHAR(255),
   `desc` VARCHAR(255),
   gmtCreate datetime,
   gmtModified datetime,
   creator VARCHAR(255),
   operator VARCHAR(255),
   herperUrlRegexes  TEXT,
   contentUrlRegexes TEXT,
   partJson TEXT,
   type int,
   enableJS int,
   cookies VARCHAR(50),
   userAgent VARCHAR(50),
   charset VARCHAR(50),
   sleep int,
   retry int,
   code_config TEXT,
   `delete` int DEFAULT 0,
   parsefields TEXT,
   contextFields TEXT,
   primary key (id)
);
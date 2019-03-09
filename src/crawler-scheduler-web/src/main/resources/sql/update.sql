--配置化爬虫表新增页面类型 20180302
alter table sw_module_edit add pageType VARCHAR(255) default "default";
alter table sw_module_pub add pageType VARCHAR(255) default "default";
--监控表
alter table monitor_record add jobCategory integer default 0;
alter table monitor_record add parentUrl VARCHAR(255) default "";
alter table monitor_record add requestDepth VARCHAR(255) default "";
alter table monitor_record add moduleName VARCHAR(255) default "";
alter table monitor_record add groupId VARCHAR(255) default "";
alter table monitor_record add serverIp VARCHAR(255) default "";
--增加任务是否使用代理ip
alter table crawler_job add useProxy int default 0;
alter table crawler_job add ipPool TEXT;
--2018-09-28 代理ip功能脚本
create table crawler_proxy_ip
(
   id bigint(20) not null AUTO_INCREMENT,
   host VARCHAR(255),
   port int,
   username VARCHAR(255),
   password VARCHAR(255),
   region VARCHAR(255),
   network VARCHAR(255),
   `desc` VARCHAR(2555),
   tag VARCHAR(2555),
   gmtCreate datetime,
   gmtModified datetime,
   creator VARCHAR(255),
   operator VARCHAR(255),
   `delete` int default 0,
   primary key (id)
);
create table crawler_proxy_group
(
   id bigint(20) not null AUTO_INCREMENT,
   name VARCHAR(255),
   `desc` VARCHAR(2555),
   gmtCreate datetime,
   gmtModified datetime,
   creator VARCHAR(255),
   operator VARCHAR(255),
   tag VARCHAR(255),
   `delete` int default 0,
   num int default 5,
   duration int default 7,
   primary key (id)
);

create table crawler_proxy_ip_group
(
  id bigint(20) not null AUTO_INCREMENT,
  proxyIpId int,
  proxyGroupId int,
  gmtCreate datetime,
  operator VARCHAR(255),
  primary key (id)
);
alter table crawler_job add expectRefreshTime datetime default CURRENT_TIMESTAMP;
alter table crawler_job add proxyGroupId int;
--2018-09-28 代理ip功能脚本
###一.linux按照安装KDC服务
####1.在Cloudera Manager服务器上安装KDC服务

    yum -y install krb5-server krb5-libs krb5-auth-dialog krb5-workstation
####2.修改/etc/krb5.conf配置

    [logging]
     default = FILE:/var/log/krb5libs.log
     kdc = FILE:/var/log/krb5kdc.log
     admin_server = FILE:/var/log/kadmind.log
    [libdefaults]
     dns_lookup_realm = false
     dns_lookup_kdc = false
     ticket_lifetime = 24h
     renew_lifetime = 7d
     forwardable = true
     rdns = false
     default_realm = HANFEI.COM
    # default_ccache_name = KEYRING:persistent:%{uid}

    [realms]
     HANFEI.COM = {
       kdc = 192.168.44.133
       admin_server = 192.168.44.133
     }

    [domain_realm]
     .hanfei.com = HANFEI.COM
     hanfei.com = HANFEI.COM


####3.修改/var/kerberos/krb5kdc/kadm5.acl配置

    [root@cdh1 krb5kdc]# cd /var/kerberos/krb5kdc
    [root@cdh1 krb5kdc]# vim kadm5.acl
    修改
    */admin@HANFEI.COM      *

####4.修改/var/kerberos/krb5kdc/kdc.conf配置

    [root@cdh1 krb5kdc]# vim kdc.conf
     [kdcdefaults]
      kdc_ports = 88
      kdc_tcp_ports = 88

     [realms]
      HANFEI.COM = {
        #master_key_type = aes256-cts
        max_renewable_life= 7d 0h 0m 0s
        acl_file = /var/kerberos/krb5kdc/kadm5.acl
        dict_file = /usr/share/dict/words
        admin_keytab = /var/kerberos/krb5kdc/kadm5.keytab
        supported_enctypes = aes256-cts:normal aes128-cts:normal des3-hmac-sha1:normal arcfour-hmac:normal camellia256-cts:normal camellia128-cts:normal des-hmac-sha1:normal des-cbc-md5:normal des-cbc-crc:normal
     }

####5.创建Kerberos数据库

    [root@cdh1 krb5kdc]# kdb5_util create –r HANFEI.COM -s
  ![](https://i.imgur.com/bNRhTPM.png)

  此处需要输入Kerberos数据库的密码

####6.创建Kerberos的管理账号

     [root@cdh1 krb5kdc]# kadmin.local
	     addprinc admin/admin@HANFEI.COM
   ![](https://i.imgur.com/3Pr2JlY.png)

   exit退出

标红部分为Kerberos管理员账号，需要输入管理员密码

####7.将Kerberos服务添加到自启动服务，并启动krb5kdc和kadmin服务

     [root@cdh1 krb5kdc]# systemctl enable krb5kdc.service
     [root@cdh1 krb5kdc]# systemctl start krb5kdc.service
     [root@cdh1 krb5kdc]# systemctl start kadmin.service
     [root@cdh1 krb5kdc]# systemctl enable kadmin.service

####8.测试Kerberos的管理员账号

     [root@cdh1 krb5kdc]# kinit admin/admin@HANFEI.COM
     [root@cdh1 krb5kdc]# klist
   ![](https://i.imgur.com/QQ5mqZ8.png)

####9.为集群安装所有Kerberos客户端，包括Cloudera Manager
  集群所有节点安装Kerberos客户端

     [root@cdh1 cdh]# yum -y install krb5-libs krb5-workstation

####10.在Cloudera Manager Server服务器上安装额外的包

    [root@cdh1 cdh]# yum -y install openldap-clients

####11.将KDC Server上的krb5.conf文件拷贝到所有Kerberos客户端
  将Kerberos服务端的krb5.conf配置文件拷贝至集群所有节点的/etc目录下：

     [root@cdh1 etc]# scp -r krb5.conf root@cdh2:/etc/
     [root@cdh1 etc]# scp -r krb5.conf root@cdh3:/etc/


###二.CDH集群启用Kerberos
####1.在KDC中给Cloudera Manager添加管理员账号

     [root@cdh1 etc]# kadmin.local
     Authenticating as principal admin/admin@HANFEI.COM with password.
     kadmin.local:  addprinc cloudera-scm/admin@HANFEI.COM
     WARNING: no policy specified for cloudera-scm/admin@HANFEI.COM; defaulting to no policy
     Enter password for principal "cloudera-scm/admin@HANFEI.COM": 
     Re-enter password for principal "cloudera-scm/admin@HANFEI.COM": 
     Principal "cloudera-scm/admin@HANFEI.COM" created.
     kadmin.local:  exit

####2.进入Cloudera Manager的“管理” --> “安全”界面
 ![](https://i.imgur.com/EOmQNfG.png)

####3.选择“启用Kerberos”，进入如下界面
 ![](https://i.imgur.com/aWXURoV.png)

####4.确保如下列出的所有检查项都已完成
 ![](https://i.imgur.com/5DYB1U7.png)

####5.点击“继续”，配置相关的KDC信息，包括类型、KDC服务器、KDC Realm、加密类型以及待创建的Service Principal（hdfs，yarn,，hbase，hive等）的更新生命期等
![](https://i.imgur.com/GDoPibs.png)

####6.不建议让Cloudera Manager来管理krb5.conf, 点击“继续”
![](https://i.imgur.com/HkrV4wY.png)

####7.输入Cloudera Manager的Kerbers管理员账号，一定得和之前创建的账号一致，点击“继续”
![](https://i.imgur.com/Rwg010a.png)

####8.点击“继续”启用Kerberos
![](https://i.imgur.com/HSvgKvR.png)

####9.Kerberos启用完成，点击“继续”
 ![](https://i.imgur.com/m0lQGkC.png)

####10.勾选重启集群，点击“继续”
![](https://i.imgur.com/Vmp1sSx.png)

####11.集群重启完成，点击“继续”

####12.点击“继续”
点击“完成”，至此已成功启用Kerberos

###三.Kerberos使用
使用han用户运行kafka命令，需要在集群所有节点创建han用户
####1.使用kadmin创建一个han的principal

    [root@cdh1 etc]# kadmin.local
     Authenticating as principal root/admin@HANFEI.COM with password.
     kadmin.local:  addprinc -randkey han@HANFEI.COM
     WARNING: no policy specified for han@HANFEI.COM; defaulting to no policy
     Enter password for principal "han@HANFEI.COM": 
     Re-enter password for principal "han@HANFEI.COM": 
     Principal "han@HANFEI.COM" created.
####2.生成keytab文件

     [root@cdh1 etc]# kadmin.local -q "xst -norandkey -k han.keytab han@HANFEI.COM"

####3.使用han用户登录Kerberos

    [root@cdh1 etc]# kdestroy
    [root@cdh1 etc]# kinit -k -t han.keytab han@HANFEI.COM
    Password for han@HANFEI.COM:

    [root@cdh1 etc]# klist
     Ticket cache: FILE:/tmp/krb5cc_0
     Default principal: han@HANFEI.COM
     Valid starting       Expires              Service principal
     12/17/2018 16:47:07  12/18/2018 16:47:07  krbtgt/HANFEI.COM@HANFEI.COM
	renew until 12/24/2018 16:47:07

###四.其他
####   1.删除用户

    [root@cdh1 etc]# kadmin.local -q "delprinc user@HANFEI.COM"

 #### 2.查询用户
   
    [root@cdh1 etc]# kadmin.local -q "list_principals"

####3.说明
   Kerberos认证登陆有2种方式，第一种是直接用户名密码登陆；第二种通过keytab文件。在程序当中，通过keytab文件认证，即可实现Kerberos认证登陆。

Kafka中启用Kerberos以及如何配置Kafka Kerberos认证的相关信息有时间将进一步的介绍。
    
     






   

  
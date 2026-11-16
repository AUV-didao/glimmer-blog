FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml ./
COPY src ./src
RUN mvn -B -DskipTests package \
    && mkdir /build/blog \
    && cd /build/blog \
    && jar xf /build/target/blog.war

FROM tomcat:9.0-jdk17-temurin
# 部署解压后的应用，让头像和封面上传可以使用 ServletContext.getRealPath。
COPY --from=build /build/blog/ /usr/local/tomcat/webapps/blog/
COPY sql/blog-seed.db /opt/glimmer/blog-seed.db
COPY --chmod=755 docker/entrypoint.sh /opt/glimmer/entrypoint.sh
ENV BLOG_DB_PATH=/var/lib/glimmer/blog.db
EXPOSE 8080
ENTRYPOINT ["/opt/glimmer/entrypoint.sh"]
CMD ["catalina.sh", "run"]

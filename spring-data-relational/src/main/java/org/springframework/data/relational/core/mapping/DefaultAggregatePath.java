/*
 * Copyright 2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.data.relational.core.mapping;

import java.util.Objects;

import org.springframework.data.mapping.PersistentPropertyPath;
import org.springframework.data.relational.core.sql.SqlIdentifier;
import org.springframework.data.util.Lazy;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Represents a path within an aggregate starting from the aggregate root.
 *
 * @since 3.2
 * @author Jens Schauder
 */
class DefaultAggregatePath  implements AggregatePath{

	private final RelationalMappingContext context;

	@Nullable private final RelationalPersistentEntity<?> rootType;

	@Nullable private final PersistentPropertyPath<? extends RelationalPersistentProperty> path;

	DefaultAggregatePath(RelationalMappingContext context,
								PersistentPropertyPath<? extends RelationalPersistentProperty> path) {

		Assert.notNull(context, "context must not be null");
		Assert.notNull(path, "path must not be null");

		this.context = context;
		this.path = path;

		this.rootType = null;
	}

	DefaultAggregatePath(RelationalMappingContext context, RelationalPersistentEntity<?> rootType) {

		Assert.notNull(context, "context must not be null");
		Assert.notNull(rootType, "rootType must not be null");

		this.context = context;
		this.rootType = rootType;

		this.path = null;
	}

	public static boolean isWritable(@Nullable PersistentPropertyPath<? extends RelationalPersistentProperty> path) {
		return path == null || path.getLeafProperty().isWritable() && isWritable(path.getParentPath());
	}

	public boolean isRoot() {
		return path == null;
	}


	/**
	 * Returns the path that has the same beginning but is one segment shorter than this path.
	 *
	 * @return the parent path. Guaranteed to be not {@literal null}.
	 * @throws IllegalStateException when called on an empty path.
	 */
	public AggregatePath getParentPath() {

		if (isRoot()) {
			throw new IllegalStateException("The parent path of a root path is not defined.");
		}

		if (path.getLength() == 1) {
			return context.getAggregatePath(path.getLeafProperty().getOwner());
		}

		return context.getAggregatePath(path.getParentPath());
	}

	/**
	 * The {@link RelationalPersistentEntity} associated with the leaf of this path.
	 *
	 * @return Might return {@literal null} when called on a path that does not represent an entity.
	 */
	@Nullable
	public RelationalPersistentEntity<?> getLeafEntity() {
		return isRoot() ? rootType : context.getPersistentEntity(path.getLeafProperty().getActualType());
	}

	/**
	 * The {@link RelationalPersistentEntity} associated with the leaf of this path or throw {@link IllegalStateException}
	 * if the leaf cannot be resolved.
	 *
	 * @return the required {@link RelationalPersistentEntity} associated with the leaf of this path.
	 * @throws IllegalStateException if the persistent entity cannot be resolved.
	 */
	public RelationalPersistentEntity<?> getRequiredLeafEntity() {

		RelationalPersistentEntity<?> entity = getLeafEntity();

		if (entity == null) {

			throw new IllegalStateException(
					String.format("Couldn't resolve leaf PersistentEntity for type %s", path.getLeafProperty().getActualType()));
		}

		return entity;
	}


	public RelationalPersistentProperty getRequiredIdProperty() {
		return isRoot() ? rootType.getRequiredIdProperty() : getRequiredLeafEntity().getRequiredIdProperty();

	}

	public int getLength() {
		return isRoot() ? 0 : path.getLength();
	}

	/**
	 * Returns {@literal true} exactly when the path is non empty and the leaf property an embedded one.
	 *
	 * @return if the leaf property is embedded.
	 */
	public boolean isEmbedded() {
		return !isRoot() && path.getLeafProperty().isEmbedded();
	}

	/**
	 * @return {@literal true} when this is an empty path or the path references an entity.
	 */
	public boolean isEntity() {
		return isRoot() || path.getLeafProperty().isEntity();
	}

	@Override
	public String toString() {
		return "AggregatePath["
				+ (rootType == null ? path.getBaseProperty().getOwner().getType().getName() : rootType.getName()) + "]"
				+ ((isRoot()) ? "/" : path.toDotPath());
	}


	public String toDotPath() {
		return isRoot() ? "" : path.toDotPath();
	}

	/**
	 * Returns {@literal true} if there are multiple values for this path, i.e. if the path contains at least one element
	 * that is a collection and array or a map.
	 *
	 * @return {@literal true} if the path contains a multivalued element.
	 */
	public boolean isMultiValued() {

		return !isRoot() && //
				(path.getLeafProperty().isCollectionLike() //
						|| path.getLeafProperty().isQualified() //
						|| getParentPath().isMultiValued() //
				);
	}

	/**
	 * @return {@literal true} if the leaf property of this path is a {@link java.util.Map}.
	 * @see RelationalPersistentProperty#isMap()
	 */
	public boolean isMap() {
		return !isRoot() && path.getLeafProperty().isMap();
	}

	/**
	 * @return {@literal true} when this is references a {@link java.util.List} or {@link java.util.Map}.
	 */
	public boolean isQualified() {
		return !isRoot() && path.getLeafProperty().isQualified();
	}

	public RelationalPersistentProperty getRequiredLeafProperty() {

		if (isRoot()) {
			throw new IllegalStateException("root path does not have a leaf property");
		}
		return path.getLeafProperty();
	}

	public RelationalPersistentProperty getBaseProperty() {

		if (isRoot()) {
			throw new IllegalStateException("root path does not have a base property");
		}
		return path.getBaseProperty();
	}

	/**
	 * @return {@literal true} when this is references a {@link java.util.Collection} or an array.
	 */
	public boolean isCollectionLike() {
		return !isRoot() && path.getLeafProperty().isCollectionLike();
	}

	/**
	 * @return whether the leaf end of the path is ordered, i.e. the data to populate must be ordered.
	 * @see RelationalPersistentProperty#isOrdered()
	 */
	public boolean isOrdered() {
		return !isRoot() && path.getLeafProperty().isOrdered();
	}

	/**
	 * Creates a new path by extending the current path by the property passed as an argument.
	 *
	 * @param property must not be {@literal null}.
	 * @return Guaranteed to be not {@literal null}.
	 */
	public AggregatePath append(RelationalPersistentProperty property) {

		PersistentPropertyPath<? extends RelationalPersistentProperty> newPath = isRoot() //
				? context.getPersistentPropertyPath(property.getName(), rootType.getType()) //
				: context.getPersistentPropertyPath(path.toDotPath() + "." + property.getName(),
						path.getBaseProperty().getOwner().getType());

		return context.getAggregatePath(newPath);
	}

	public PersistentPropertyPath<? extends RelationalPersistentProperty> getRequiredPersistentPropertyPath() {

		Assert.state(!isRoot(), "path must not be null");
		return path;
	}

	@Nullable
	public PersistentPropertyPathExtension getPathExtension() {

		if (isRoot()) {
			return new PersistentPropertyPathExtension(context, rootType);
		}
		return new PersistentPropertyPathExtension(context, path);
	}

	@Override
	public boolean equals(Object o) {

		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		DefaultAggregatePath that = (DefaultAggregatePath) o;
		return Objects.equals(context, that.context) && Objects.equals(rootType, that.rootType) && Objects.equals(path, that.path);
	}

	@Override
	public int hashCode() {

		return Objects.hash(context, rootType, path);
	}
}

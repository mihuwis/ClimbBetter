using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata.Builders;
using ClimbBetter.Domain.Training;

namespace ClimbBetter.Infrastructure.Persistence.Configurations;

public class LocationConfiguration : IEntityTypeConfiguration<Location>
{
    public void Configure(EntityTypeBuilder<Location> builder)
    {
        builder.ToTable("Locations");

        builder.HasData(
            new Location
            {
                Id = new Guid("11111111-1111-1111-1111-111111111111"),
                Name = "Bronx",
                Continent = "Europe",
                Country = "PL",
                Area = "Kraków",
                Sector = null,
                SubArea = null,
                Type = LocationType.BoulderGym,
                IsPublic = true,
                OwnerId = null
            },
            new Location
            {
                Id = new Guid("22222222-2222-2222-2222-222222222222"),
                Name = "Filar Abazego",
                Continent = "Europe",
                Country = "PL",
                Area = "Jura Krakowsko-Częstochowska",
                Sector = "Dolina Bolechowicka",
                SubArea = "Filar Abazego",
                Type = LocationType.SportCrag,
                IsPublic = true,
                OwnerId = null
            }
        );
    }
}
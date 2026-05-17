using ClimbBetter.Domain.Training;
using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata.Builders;


namespace ClimbBetter.Infrastructure.Persistence.Configurations;

public class ClimbConfiguration : IEntityTypeConfiguration<Climb>
{
    public void Configure(EntityTypeBuilder<Climb> builder)
    {
        builder.ToTable("Climbs");

        builder.HasData(

            // ===== Bronx =====
            new Climb
            {
                Id = new Guid("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1"),
                Name = "Żółta 1",
                Type = ClimbType.Boulder,
                LocationId = new Guid("11111111-1111-1111-1111-111111111111"),
                DifficultyId = 2, // 6a
                LengthMeters = null,
                SuggestedMoveCount = 7,
                IsPublic = true,
                OwnerId = null
            },
            new Climb
            {
                Id = new Guid("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2"),
                Name = "Żółta 2",
                Type = ClimbType.Boulder,
                LocationId = new Guid("11111111-1111-1111-1111-111111111111"),
                DifficultyId = 4, // 6b
                LengthMeters = null,
                SuggestedMoveCount = 7,
                IsPublic = true,
                OwnerId = null
            },
            new Climb
            {
                Id = new Guid("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3"),
                Name = "Żółta 3",
                Type = ClimbType.Boulder,
                LocationId = new Guid("11111111-1111-1111-1111-111111111111"),
                DifficultyId = 8, // 7a
                LengthMeters = null,
                SuggestedMoveCount = 7,
                IsPublic = true,
                OwnerId = null
            },

            // ===== Filar Abazego =====
            new Climb
            {
                Id = new Guid("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1"),
                Name = "Ryski nad tablicą",
                Type = ClimbType.Route,
                LocationId = new Guid("22222222-2222-2222-2222-222222222222"),
                DifficultyId = 2, // 6a
                LengthMeters = 22,
                SuggestedMoveCount = 44,
                IsPublic = true,
                OwnerId = null
            },
            new Climb
            {
                Id = new Guid("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb2"),
                Name = "Filar Wallischa",
                Type = ClimbType.Route,
                LocationId = new Guid("22222222-2222-2222-2222-222222222222"),
                DifficultyId = 4, // 6b
                LengthMeters = 20,
                SuggestedMoveCount = 40,
                IsPublic = true,
                OwnerId = null
            },
            new Climb
            {
                Id = new Guid("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb3"),
                Name = "Zacięcie w Abazym",
                Type = ClimbType.Route,
                LocationId = new Guid("22222222-2222-2222-2222-222222222222"),
                DifficultyId = 7, // 6c+
                LengthMeters = 22,
                SuggestedMoveCount = 44,
                IsPublic = true,
                OwnerId = null
            },
            new Climb
            {
                Id = new Guid("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb4"),
                Name = "Ryski Myszkowskiego",
                Type = ClimbType.Route,
                LocationId = new Guid("22222222-2222-2222-2222-222222222222"),
                DifficultyId = 7, // 6c+
                LengthMeters = 22,
                SuggestedMoveCount = 44,
                IsPublic = true,
                OwnerId = null
            },
            new Climb
            {
                Id = new Guid("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb5"),
                Name = "Abazy Pasza",
                Type = ClimbType.Route,
                LocationId = new Guid("22222222-2222-2222-2222-222222222222"),
                DifficultyId = 8, // 7a
                LengthMeters = 22,
                SuggestedMoveCount = 44,
                IsPublic = true,
                OwnerId = null
            },
            new Climb
            {
                Id = new Guid("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb6"),
                Name = "Zacięcie w Abazym wprost",
                Type = ClimbType.Route,
                LocationId = new Guid("22222222-2222-2222-2222-222222222222"),
                DifficultyId = 8, // 7a
                LengthMeters = 22,
                SuggestedMoveCount = 44,
                IsPublic = true,
                OwnerId = null
            }
        );
    }
}